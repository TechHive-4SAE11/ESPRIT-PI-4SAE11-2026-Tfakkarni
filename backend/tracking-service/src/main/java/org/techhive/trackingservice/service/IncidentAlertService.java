package org.techhive.trackingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.techhive.trackingservice.dto.NotificationResponse;
import org.techhive.trackingservice.entity.DoctorNotification;
import org.techhive.trackingservice.entity.IncidentEntry;
import org.techhive.trackingservice.repository.DoctorNotificationRepository;
import org.techhive.trackingservice.repository.MedicalFolderRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IncidentAlertService {

    private final RestTemplate lbRestTemplate;
    private final RestTemplate plainRestTemplate;
    private final DoctorNotificationRepository notificationRepo;
    private final MedicalFolderRepository medicalFolderRepo;

    @Autowired(required = false)
    @Lazy
    private TelegramAlertService telegramAlertService;

    @Value("${mailtrap.token}")
    private String mailtrapToken;

    @Value("${mailtrap.inbox-id}")
    private String inboxId;

    @Value("${mailtrap.from:noreply@tfakkarni.com}")
    private String fromEmail;

    public IncidentAlertService(
            @Qualifier("lbRestTemplate")    RestTemplate lbRestTemplate,
            @Qualifier("plainRestTemplate") RestTemplate plainRestTemplate,
            DoctorNotificationRepository notificationRepo,
            MedicalFolderRepository medicalFolderRepo) {
        this.lbRestTemplate    = lbRestTemplate;
        this.plainRestTemplate = plainRestTemplate;
        this.notificationRepo  = notificationRepo;
        this.medicalFolderRepo = medicalFolderRepo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main alert handler
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    public void handleIncidentAlert(IncidentEntry incident,
                                    String patientKeycloakId,
                                    String logDate) {

        String severity = incident.getSeverity();
        if (!"MODERE".equalsIgnoreCase(severity) && !"GRAVE".equalsIgnoreCase(severity)) {
            return;
        }

        log.info("🚨 Processing alert — patient={} severity={}", patientKeycloakId, severity);

        // ── 1. Find doctor ──────────────────────────────────────────────────
        var folders = medicalFolderRepo.findByIdPatient(patientKeycloakId);
        if (folders.isEmpty()) {
            log.warn("⚠️  No medical folder for patient {} — alert aborted.", patientKeycloakId);
            return;
        }
        String doctorKeycloakId = folders.get(0).getIdDoctor();

        // ── 2. Fetch user info ──────────────────────────────────────────────
        Map<String, String> patientInfo = fetchUserInfo(patientKeycloakId);
        Map<String, String> doctorInfo  = fetchUserInfo(doctorKeycloakId);

        String patientName = fullName(patientInfo, patientKeycloakId);
        String doctorName  = fullName(doctorInfo,  doctorKeycloakId);
        String doctorEmail = doctorInfo.getOrDefault("email", "");

        log.info("   patient='{}' doctor='{}' email='{}'", patientName, doctorName, doctorEmail);

        // ── 3. Save DB notification ─────────────────────────────────────────
        try {
            DoctorNotification notif = new DoctorNotification();
            notif.setDoctorKeycloakId(doctorKeycloakId);
            notif.setPatientKeycloakId(patientKeycloakId);
            notif.setPatientName(patientName);
            notif.setIncidentType(incident.getIncidentType());
            notif.setSeverity(severity);
            notif.setDescription(incident.getDescription());
            notif.setLocation(incident.getLocation());
            notif.setActionTaken(incident.getActionTaken());
            notif.setOccurredAt(incident.getOccurredAt());
            notif.setLogDate(logDate);
            notif.setRead(false);
            notificationRepo.save(notif);
            log.info("   ✅ Notification saved to DB");
        } catch (Exception e) {
            log.error("   ❌ Failed to save notification: {}", e.getMessage(), e);
        }

        // ── 4. Send email (MODERE + GRAVE) ──────────────────────────────────
        if (!doctorEmail.isBlank()) {
            try {
                sendAlertEmail(doctorEmail, doctorName, patientName, incident, logDate);
            } catch (Exception e) {
                log.error("   ❌ Email error: {}", e.getMessage(), e);
            }
        }

        // ── 5. Telegram alert (GRAVE only) ──────────────────────────────────
        if ("GRAVE".equalsIgnoreCase(severity) && telegramAlertService != null) {
            try {
                telegramAlertService.sendGraveAlert(
                        null,  // use default chat ID from config
                        doctorName,
                        patientName,
                        incident.getIncidentType(),
                        incident.getDescription(),
                        incident.getLocation(),
                        incident.getActionTaken(),
                        logDate
                );
            } catch (Exception e) {
                log.error("   ❌ Telegram alert error: {}", e.getMessage(), e);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification CRUD
    // ─────────────────────────────────────────────────────────────────────────

    public List<NotificationResponse> getNotificationsForDoctor(String doctorKeycloakId) {
        return notificationRepo
                .findByDoctorKeycloakIdOrderByCreatedAtDesc(doctorKeycloakId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long getUnreadCount(String doctorKeycloakId) {
        return notificationRepo.countByDoctorKeycloakIdAndReadFalse(doctorKeycloakId);
    }

    public NotificationResponse markAsRead(Long notificationId) {
        DoctorNotification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        n.setRead(true);
        n.setReadAt(LocalDateTime.now());
        return toResponse(notificationRepo.save(n));
    }

    public void markAllAsRead(String doctorKeycloakId) {
        List<DoctorNotification> unread = notificationRepo
                .findByDoctorKeycloakIdOrderByCreatedAtDesc(doctorKeycloakId)
                .stream().filter(n -> !n.isRead()).collect(Collectors.toList());
        unread.forEach(n -> { n.setRead(true); n.setReadAt(LocalDateTime.now()); });
        notificationRepo.saveAll(unread);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User info fetch
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, String> fetchUserInfo(String keycloakId) {
        try {
            Map<?, ?> resp = lbRestTemplate.getForObject(
                    "http://user-service/api/users/keycloak/" + keycloakId, Map.class);
            if (resp != null) {
                Map<String, String> info = new HashMap<>();
                info.put("firstName", str(resp, "firstName"));
                info.put("lastName",  str(resp, "lastName"));
                info.put("email",     str(resp, "email"));
                return info;
            }
        } catch (Exception e) {
            log.warn("   Could not fetch user info for {}: {}", keycloakId, e.getMessage());
        }
        return new HashMap<>();
    }

    private String str(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return (v instanceof String s) ? s : "";
    }

    private String fullName(Map<String, String> info, String fallback) {
        String fn   = info.getOrDefault("firstName", "").trim();
        String ln   = info.getOrDefault("lastName",  "").trim();
        String full = (fn + " " + ln).trim();
        return full.isBlank() ? fallback : full;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Email
    // ─────────────────────────────────────────────────────────────────────────

    private void sendAlertEmail(String doctorEmail, String doctorName,
                                String patientName, IncidentEntry incident,
                                String logDate) {

        String url = "https://sandbox.api.mailtrap.io/api/send/" + inboxId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mailtrapToken);

        boolean isGrave   = "GRAVE".equalsIgnoreCase(incident.getSeverity());
        String subjectTag = isGrave ? "GRAVE 🔴" : "MODÉRÉ 🟠";

        Map<String, Object> body = new HashMap<>();
        body.put("from",    Map.of("email", fromEmail, "name", "Tfakkarni Alertes"));
        body.put("to",      List.of(Map.of("email", doctorEmail)));
        body.put("subject", "⚠️ Alerte Incident " + subjectTag + " — " + patientName);
        body.put("html",    buildAlertHtml(doctorName, patientName, incident, logDate));
        body.put("text",    buildAlertText(patientName, incident, logDate));

        ResponseEntity<Map> resp = plainRestTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        log.info("   ✅ Email sent to {} — HTTP {}", doctorEmail, resp.getStatusCode());
    }

    private String buildAlertHtml(String doctorName, String patientName,
                                  IncidentEntry incident, String logDate) {

        boolean isGrave      = "GRAVE".equalsIgnoreCase(incident.getSeverity());
        String severityLabel = isGrave ? "GRAVE"  : "MODÉRÉ";
        String gradStart     = isGrave ? "#dc2626" : "#f97316";
        String gradEnd       = isGrave ? "#b91c1c" : "#ea580c";
        String accent        = isGrave ? "#dc2626" : "#f97316";
        String lightBg       = isGrave ? "#fef2f2" : "#fff7ed";
        String borderClr     = isGrave ? "#fca5a5" : "#fed7aa";

        String dateFormatted = logDate;
        try {
            dateFormatted = java.time.LocalDate.parse(logDate)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {}

        StringBuilder extra = new StringBuilder();
        if (hasValue(incident.getLocation()))
            extra.append(row("📍 Lieu",         incident.getLocation(),      accent, borderClr));
        if (hasValue(incident.getInjuryDetails()))
            extra.append(row("🩹 Blessures",    incident.getInjuryDetails(), accent, borderClr));
        if (hasValue(incident.getActionTaken()))
            extra.append(row("✅ Action prise", incident.getActionTaken(),   accent, borderClr));

        String urgency = isGrave
            ? "Un incident de gravité <b>GRAVE</b> a été enregistré pour votre patient."
              + " Une intervention médicale immédiate est requise."
            : "Un incident de gravité <b>MODÉRÉ</b> a été enregistré pour votre patient."
              + " Veuillez consulter le dossier dès que possible.";

        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'></head>"
            + "<body style='margin:0;padding:0;background:#f0f4f8;font-family:Arial,Helvetica,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f4f8;padding:32px 16px;'>"
            + "<tr><td align='center'>"
            + "<table width='520' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:16px;"
            +   "overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,.12);'>"
            // HEADER
            + "<tr><td style='background:linear-gradient(135deg," + gradStart + "," + gradEnd + ");"
            +   "padding:36px 24px;text-align:center;'>"
            + "<div style='display:inline-block;background:rgba(255,255,255,.2);border-radius:50%;"
            +   "width:64px;height:64px;line-height:64px;font-size:32px;margin-bottom:16px;'>⚠️</div>"
            + "<h1 style='color:#fff;margin:0;font-size:22px;font-weight:800;'>Alerte Incident " + severityLabel + "</h1>"
            + "<p style='color:rgba(255,255,255,.85);margin:8px 0 0;font-size:13px;'>Plateforme tfakkarni – Suivi Alzheimer</p>"
            + "</td></tr>"
            // BODY
            + "<tr><td style='padding:32px 32px 8px;'>"
            + "<p style='color:#1f2937;font-size:15px;margin:0 0 6px;'>Bonjour <b>Dr. " + esc(doctorName) + "</b>,</p>"
            + "<p style='color:#6b7280;font-size:14px;line-height:1.7;margin:0 0 24px;'>" + urgency + "</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='border:1px solid " + borderClr + ";"
            +   "border-radius:10px;overflow:hidden;background:" + lightBg + ";margin-bottom:20px;'>"
            + row("👤 Patient",          patientName,               accent, borderClr)
            + row("🏥 Type d'incident",  incident.getIncidentType(), accent, borderClr)
            + row("📅 Date",             dateFormatted,             accent, borderClr)
            + row("📋 Description",      incident.getDescription(), accent, borderClr)
            + extra
            + "</table>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#fefce8;"
            +   "border:1px solid #fde047;border-radius:10px;margin-bottom:8px;'>"
            + "<tr><td style='padding:14px 18px;'>"
            + "<p style='color:#854d0e;font-size:13px;margin:0;line-height:1.6;'>"
            +   "📋 <b>Action requise :</b> Veuillez consulter le dossier médical sur la plateforme tfakkarni.</p>"
            + "</td></tr></table>"
            + "</td></tr>"
            // FOOTER
            + "<tr><td style='background:#f9fafb;border-top:1px solid #e5e7eb;padding:14px 24px;text-align:center;'>"
            + "<p style='color:#9ca3af;font-size:11px;margin:0;'>© 2026 Tfakkarni – Plateforme de suivi des patients Alzheimer</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    private String row(String label, String value, String accent, String borderClr) {
        return "<tr>"
            + "<td style='padding:11px 16px;border-bottom:1px solid " + borderClr + ";"
            +   "color:" + accent + ";font-size:13px;font-weight:700;width:38%;'>" + esc(label) + "</td>"
            + "<td style='padding:11px 16px;border-bottom:1px solid " + borderClr + ";"
            +   "color:#111827;font-size:13px;'>" + esc(value) + "</td>"
            + "</tr>";
    }

    private String buildAlertText(String patientName, IncidentEntry incident, String logDate) {
        return "ALERTE INCIDENT " + incident.getSeverity() + "\n\n"
            + "Patient : "     + patientName                + "\n"
            + "Date : "        + logDate                    + "\n"
            + "Type : "        + incident.getIncidentType() + "\n"
            + "Description : " + incident.getDescription()  + "\n"
            + (hasValue(incident.getLocation())      ? "Lieu : "         + incident.getLocation()      + "\n" : "")
            + (hasValue(incident.getInjuryDetails()) ? "Blessures : "    + incident.getInjuryDetails() + "\n" : "")
            + (hasValue(incident.getActionTaken())   ? "Action prise : " + incident.getActionTaken()   + "\n" : "")
            + "\n— Tfakkarni";
    }

    private boolean hasValue(String s) { return s != null && !s.isBlank(); }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private NotificationResponse toResponse(DoctorNotification n) {
        NotificationResponse r = new NotificationResponse();
        r.setId(n.getId());
        r.setDoctorKeycloakId(n.getDoctorKeycloakId());
        r.setPatientKeycloakId(n.getPatientKeycloakId());
        r.setPatientName(n.getPatientName());
        r.setIncidentType(n.getIncidentType());
        r.setSeverity(n.getSeverity());
        r.setDescription(n.getDescription());
        r.setLocation(n.getLocation());
        r.setActionTaken(n.getActionTaken());
        r.setOccurredAt(n.getOccurredAt());
        r.setLogDate(n.getLogDate());
        r.setRead(n.isRead());
        r.setCreatedAt(n.getCreatedAt());
        r.setReadAt(n.getReadAt());
        return r;
    }
}
