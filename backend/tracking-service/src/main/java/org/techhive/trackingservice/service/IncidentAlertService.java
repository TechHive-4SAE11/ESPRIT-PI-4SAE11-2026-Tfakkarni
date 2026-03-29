package org.techhive.trackingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
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

    // ── Config Mailtrap ───────────────────────────────────────────────────────
    @Value("${mailtrap.token}")
    private String mailtrapToken;

    @Value("${mailtrap.inbox-id}")
    private String mailtrapInboxId;

    @Value("${mailtrap.from:noreply@tfakkarni.com}")
    private String fromEmail;

    // ── Config Telegram ───────────────────────────────────────────────────────
    @Value("${telegram.bot-token:}")
    private String telegramBotToken;

    @Value("${telegram.default-chat-id:}")
    private String telegramChatId;

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
    // Point d'entrée principal — appelé par DailyMonitoringService
    // NOTE: PAS @Async — erreurs visibles dans les logs
    // ─────────────────────────────────────────────────────────────────────────
    public void handleIncidentAlert(IncidentEntry incident,
                                    String patientKeycloakId,
                                    String logDate) {
        // Copier les champs AVANT tout appel externe (évite problèmes Hibernate lazy)
        String severity     = incident.getSeverity();
        String incidentType = incident.getIncidentType();
        String description  = incident.getDescription();
        String location     = incident.getLocation();
        String actionTaken  = incident.getActionTaken();
        String injuryDet    = incident.getInjuryDetails();
        String occurredAt   = incident.getOccurredAt();

        if (!"MODERE".equalsIgnoreCase(severity) && !"GRAVE".equalsIgnoreCase(severity)) {
            log.debug("Severity '{}' ne nécessite pas d'alerte.", severity);
            return;
        }

        log.info("═══════════════════════════════════════════");
        log.info("🚨 INCIDENT ALERT — severity={} patient={}", severity, patientKeycloakId);
        log.info("═══════════════════════════════════════════");

        // ── 1. Trouver le médecin via le dossier médical ────────────────────
        var folders = medicalFolderRepo.findByIdPatient(patientKeycloakId);
        log.info("   Dossiers médicaux trouvés: {}", folders.size());

        if (folders.isEmpty()) {
            log.warn("   ⚠️  Aucun dossier médical pour patient={} — alerte annulée.", patientKeycloakId);
            return;
        }
        String doctorKeycloakId = folders.get(0).getIdDoctor();
        log.info("   Médecin: {}", doctorKeycloakId);

        // ── 2. Récupérer les infos depuis user-service ──────────────────────
        log.info("   Appel user-service pour patient...");
        Map<String, String> patientInfo = fetchUserInfo(patientKeycloakId);
        log.info("   Appel user-service pour médecin...");
        Map<String, String> doctorInfo  = fetchUserInfo(doctorKeycloakId);

        String patientName = fullName(patientInfo, patientKeycloakId);
        String doctorName  = fullName(doctorInfo,  doctorKeycloakId);
        String doctorEmail = doctorInfo.getOrDefault("email", "");

        log.info("   patient='{}' | doctor='{}' | email='{}'", patientName, doctorName, doctorEmail);

        // ── 3. Sauvegarder la notification en DB ────────────────────────────
        try {
            DoctorNotification notif = new DoctorNotification();
            notif.setDoctorKeycloakId(doctorKeycloakId);
            notif.setPatientKeycloakId(patientKeycloakId);
            notif.setPatientName(patientName);
            notif.setIncidentType(incidentType);
            notif.setSeverity(severity);
            notif.setDescription(description);
            notif.setLocation(location);
            notif.setActionTaken(actionTaken);
            notif.setOccurredAt(occurredAt);
            notif.setLogDate(logDate);
            notif.setRead(false);
            notificationRepo.save(notif);
            log.info("   ✅ Notification sauvegardée en DB (id={})", notif.getId());
        } catch (Exception e) {
            log.error("   ❌ Échec sauvegarde DB: {}", e.getMessage(), e);
        }

        // ── 4. Email Mailtrap (MODERE + GRAVE) ──────────────────────────────
        if (!doctorEmail.isBlank()) {
            log.info("   📧 Envoi email → {}", doctorEmail);
            try {
                sendAlertEmail(doctorEmail, doctorName, patientName,
                               severity, incidentType, description,
                               location, injuryDet, actionTaken, logDate);
            } catch (Exception e) {
                log.error("   ❌ Échec email: {} — {}", e.getClass().getSimpleName(), e.getMessage(), e);
            }
        } else {
            log.warn("   ⚠️  Email vide — skipping. PatientInfo={} DoctorInfo={}", patientInfo, doctorInfo);
        }

        // ── 5. Telegram (GRAVE uniquement) ──────────────────────────────────
        if ("GRAVE".equalsIgnoreCase(severity)) {
            log.info("   📨 Envoi Telegram...");
            try {
                sendTelegramAlert(doctorName, patientName,
                                  incidentType, description,
                                  location, actionTaken, logDate);
            } catch (Exception e) {
                log.error("   ❌ Échec Telegram: {} — {}", e.getClass().getSimpleName(), e.getMessage(), e);
            }
        }

        log.info("═══════════════════════════════════════════");
        log.info("✅ Traitement alerte terminé");
        log.info("═══════════════════════════════════════════");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test endpoint direct (pour debug)
    // ─────────────────────────────────────────────────────────────────────────

    /** Teste directement email + Telegram sans passer par un incident réel */
    public Map<String, String> testAlertDirect(String email) {
        Map<String, String> results = new HashMap<>();

        // Test Email
        try {
            String url  = "https://sandbox.api.mailtrap.io/api/send/" + mailtrapInboxId;
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.setBearerAuth(mailtrapToken);

            Map<String, Object> body = new HashMap<>();
            body.put("from",    Map.of("email", fromEmail, "name", "Tfakkarni Test"));
            body.put("to",      List.of(Map.of("email", email)));
            body.put("subject", "✅ Test Email — Tfakkarni Alert System");
            body.put("html",    "<h2>Test réussi !</h2><p>Le système d'alertes email fonctionne correctement.</p>");
            body.put("text",    "Test réussi ! Le système d'alertes email fonctionne.");

            ResponseEntity<Map> resp = plainRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
            results.put("email", "✅ OK — HTTP " + resp.getStatusCode());
            log.info("TEST EMAIL: ✅ OK");
        } catch (Exception e) {
            results.put("email", "❌ FAILED — " + e.getMessage());
            log.error("TEST EMAIL: ❌ {}", e.getMessage());
        }

        // Test Telegram
        try {
            if (telegramBotToken.isBlank()) {
                results.put("telegram", "⚠️ bot-token non configuré dans application.yml");
            } else {
                String url = "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage";
                Map<String, Object> body = new HashMap<>();
                body.put("chat_id", telegramChatId);
                body.put("text",    "✅ <b>Test Tfakkarni</b> — Le bot fonctionne correctement !");
                body.put("parse_mode", "HTML");

                HttpHeaders h = new HttpHeaders();
                h.setContentType(MediaType.APPLICATION_JSON);

                ResponseEntity<Map> resp = plainRestTemplate.exchange(
                        url, HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
                boolean ok = resp.getBody() != null && Boolean.TRUE.equals(resp.getBody().get("ok"));
                results.put("telegram", ok ? "✅ OK" : "⚠️ " + resp.getBody());
                log.info("TEST TELEGRAM: {}", ok ? "✅ OK" : "⚠️ " + resp.getBody());
            }
        } catch (Exception e) {
            results.put("telegram", "❌ FAILED — " + e.getMessage());
            log.error("TEST TELEGRAM: ❌ {}", e.getMessage());
        }

        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification CRUD
    // ─────────────────────────────────────────────────────────────────────────

    public List<NotificationResponse> getNotificationsForDoctor(String doctorKeycloakId) {
        return notificationRepo.findByDoctorKeycloakIdOrderByCreatedAtDesc(doctorKeycloakId)
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
    // Telegram
    // ─────────────────────────────────────────────────────────────────────────

    private void sendTelegramAlert(String doctorName, String patientName,
                                   String incidentType, String description,
                                   String location, String actionTaken,
                                   String logDate) {

        if (telegramBotToken.isBlank() || telegramChatId.isBlank()) {
            log.warn("   ⚠️  Telegram non configuré (bot-token='{}' chat-id='{}')",
                     telegramBotToken.isBlank() ? "VIDE" : "OK",
                     telegramChatId.isBlank()   ? "VIDE" : telegramChatId);
            return;
        }

        String message = buildTelegramMessage(doctorName, patientName,
                                              incidentType, description,
                                              location, actionTaken, logDate);

        String url = "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id",    telegramChatId);
        body.put("text",       message);
        body.put("parse_mode", "HTML");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = plainRestTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        boolean ok = resp.getBody() != null && Boolean.TRUE.equals(resp.getBody().get("ok"));
        if (ok) {
            log.info("   ✅ Telegram envoyé → chat_id={}", telegramChatId);
        } else {
            log.warn("   ⚠️  Telegram réponse: {}", resp.getBody());
        }
    }

    private String buildTelegramMessage(String doctorName, String patientName,
                                        String incidentType, String description,
                                        String location, String actionTaken,
                                        String logDate) {
        String dr = (doctorName != null && !doctorName.isBlank()) ? "Dr. " + doctorName : "Docteur";

        String dateFormatted = logDate;
        try {
            dateFormatted = java.time.LocalDate.parse(logDate)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {}

        StringBuilder sb = new StringBuilder();
        sb.append("🚨 <b>ALERTE INCIDENT GRAVE — Tfakkarni</b>\n\n");
        sb.append("Bonjour ").append(esc(dr)).append(",\n\n");
        sb.append("Un incident de gravité <b>GRAVE</b> vient d'être signalé.\n\n");
        sb.append("─────────────────────────\n");
        sb.append("👤 <b>Patient :</b> ").append(esc(patientName)).append("\n");
        sb.append("📅 <b>Date :</b> ").append(esc(dateFormatted)).append("\n");
        sb.append("🏥 <b>Type :</b> ").append(esc(incidentType)).append("\n");
        if (hasValue(description))
            sb.append("📋 <b>Description :</b> ").append(esc(description)).append("\n");
        if (hasValue(location))
            sb.append("📍 <b>Lieu :</b> ").append(esc(location)).append("\n");
        if (hasValue(actionTaken))
            sb.append("✅ <b>Action prise :</b> ").append(esc(actionTaken)).append("\n");
        sb.append("─────────────────────────\n\n");
        sb.append("🔴 <b>Intervention médicale immédiate requise.</b>\n");
        sb.append("Connectez-vous à la plateforme <b>Tfakkarni</b>.");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Email Mailtrap
    // ─────────────────────────────────────────────────────────────────────────

    private void sendAlertEmail(String doctorEmail, String doctorName,
                                String patientName, String severity,
                                String incidentType, String description,
                                String location, String injuryDetails,
                                String actionTaken, String logDate) {

        String url = "https://sandbox.api.mailtrap.io/api/send/" + mailtrapInboxId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mailtrapToken);

        boolean isGrave   = "GRAVE".equalsIgnoreCase(severity);
        String subjectTag = isGrave ? "GRAVE 🔴" : "MODÉRÉ 🟠";

        Map<String, Object> body = new HashMap<>();
        body.put("from",    Map.of("email", fromEmail, "name", "Tfakkarni Alertes"));
        body.put("to",      List.of(Map.of("email", doctorEmail)));
        body.put("subject", "⚠️ Alerte Incident " + subjectTag + " — " + patientName);
        body.put("html",    buildEmailHtml(doctorName, patientName, severity,
                                          incidentType, description,
                                          location, injuryDetails, actionTaken, logDate));
        body.put("text",    buildEmailText(patientName, severity, incidentType,
                                          description, location, injuryDetails,
                                          actionTaken, logDate));

        ResponseEntity<Map> resp = plainRestTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        log.info("   ✅ Email envoyé → {} — HTTP {}", doctorEmail, resp.getStatusCode());
    }

    private String buildEmailHtml(String doctorName, String patientName,
                                  String severity, String incidentType,
                                  String description, String location,
                                  String injuryDetails, String actionTaken,
                                  String logDate) {

        boolean isGrave      = "GRAVE".equalsIgnoreCase(severity);
        String severityLabel = isGrave ? "GRAVE"   : "MODÉRÉ";
        String gradStart     = isGrave ? "#dc2626"  : "#f97316";
        String gradEnd       = isGrave ? "#b91c1c"  : "#ea580c";
        String accent        = isGrave ? "#dc2626"  : "#f97316";
        String lightBg       = isGrave ? "#fef2f2"  : "#fff7ed";
        String border        = isGrave ? "#fca5a5"  : "#fed7aa";

        String dateFormatted = logDate;
        try {
            dateFormatted = java.time.LocalDate.parse(logDate)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {}

        StringBuilder extra = new StringBuilder();
        if (hasValue(location))       extra.append(eRow("📍 Lieu",         location,      accent, border));
        if (hasValue(injuryDetails))  extra.append(eRow("🩹 Blessures",    injuryDetails, accent, border));
        if (hasValue(actionTaken))    extra.append(eRow("✅ Action prise", actionTaken,   accent, border));

        String urgency = isGrave
            ? "Un incident de gravité <b>GRAVE</b> a été enregistré. Une intervention médicale immédiate est requise."
            : "Un incident de gravité <b>MODÉRÉ</b> a été enregistré. Veuillez consulter le dossier dès que possible.";

        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'></head>"
            + "<body style='margin:0;padding:0;background:#f0f4f8;font-family:Arial,Helvetica,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f4f8;padding:32px 16px;'><tr><td align='center'>"
            + "<table width='520' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,.12);'>"
            + "<tr><td style='background:linear-gradient(135deg," + gradStart + "," + gradEnd + ");padding:36px 24px;text-align:center;'>"
            + "<div style='display:inline-block;background:rgba(255,255,255,.2);border-radius:50%;width:64px;height:64px;line-height:64px;font-size:32px;margin-bottom:16px;'>⚠️</div>"
            + "<h1 style='color:#fff;margin:0;font-size:22px;font-weight:800;'>Alerte Incident " + severityLabel + "</h1>"
            + "<p style='color:rgba(255,255,255,.85);margin:8px 0 0;font-size:13px;'>Plateforme tfakkarni – Suivi Alzheimer</p>"
            + "</td></tr>"
            + "<tr><td style='padding:32px;'>"
            + "<p style='color:#1f2937;font-size:15px;margin:0 0 8px;'>Bonjour <b>Dr. " + esc(doctorName) + "</b>,</p>"
            + "<p style='color:#6b7280;font-size:14px;line-height:1.7;margin:0 0 24px;'>" + urgency + "</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='border:1px solid " + border + ";border-radius:10px;overflow:hidden;background:" + lightBg + ";margin-bottom:20px;'>"
            + eRow("👤 Patient",     patientName,   accent, border)
            + eRow("🏥 Type",        incidentType,  accent, border)
            + eRow("📅 Date",        dateFormatted, accent, border)
            + eRow("📋 Description", description,   accent, border)
            + extra
            + "</table>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#fefce8;border:1px solid #fde047;border-radius:10px;'>"
            + "<tr><td style='padding:14px 18px;'>"
            + "<p style='color:#854d0e;font-size:13px;margin:0;'>📋 <b>Action requise :</b> Connectez-vous à la plateforme Tfakkarni pour consulter le dossier médical.</p>"
            + "</td></tr></table>"
            + "</td></tr>"
            + "<tr><td style='background:#f9fafb;border-top:1px solid #e5e7eb;padding:14px 24px;text-align:center;'>"
            + "<p style='color:#9ca3af;font-size:11px;margin:0;'>© 2026 Tfakkarni – Plateforme de suivi des patients Alzheimer</p>"
            + "</td></tr>"
            + "</table></td></tr></table></body></html>";
    }

    private String eRow(String label, String value, String accent, String border) {
        return "<tr>"
            + "<td style='padding:11px 16px;border-bottom:1px solid " + border + ";color:" + accent + ";font-size:13px;font-weight:700;width:38%;'>" + esc(label) + "</td>"
            + "<td style='padding:11px 16px;border-bottom:1px solid " + border + ";color:#111827;font-size:13px;'>" + esc(value) + "</td>"
            + "</tr>";
    }

    private String buildEmailText(String patientName, String severity,
                                  String incidentType, String description,
                                  String location, String injuryDetails,
                                  String actionTaken, String logDate) {
        return "ALERTE INCIDENT " + severity + "\n\n"
            + "Patient : "     + patientName   + "\n"
            + "Date : "        + logDate       + "\n"
            + "Type : "        + incidentType  + "\n"
            + "Description : " + description   + "\n"
            + (hasValue(location)      ? "Lieu : "         + location      + "\n" : "")
            + (hasValue(injuryDetails) ? "Blessures : "    + injuryDetails + "\n" : "")
            + (hasValue(actionTaken)   ? "Action prise : " + actionTaken   + "\n" : "")
            + "\n— Tfakkarni";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // User-service
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, String> fetchUserInfo(String keycloakId) {
        try {
            String url = "http://user-service/api/users/keycloak/" + keycloakId;
            log.info("   → GET {}", url);
            Map<?, ?> resp = lbRestTemplate.getForObject(url, Map.class);
            if (resp != null) {
                log.info("   → Réponse: keys={}", resp.keySet());
                Map<String, String> info = new HashMap<>();
                info.put("firstName", str(resp, "firstName"));
                info.put("lastName",  str(resp, "lastName"));
                info.put("email",     str(resp, "email"));
                info.put("phone",     str(resp, "phone"));
                return info;
            } else {
                log.warn("   → Réponse NULL pour keycloakId={}", keycloakId);
            }
        } catch (Exception e) {
            log.error("   → Échec fetchUserInfo pour {}: {} — {}",
                      keycloakId, e.getClass().getSimpleName(), e.getMessage());
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
    // Utils
    // ─────────────────────────────────────────────────────────────────────────

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
