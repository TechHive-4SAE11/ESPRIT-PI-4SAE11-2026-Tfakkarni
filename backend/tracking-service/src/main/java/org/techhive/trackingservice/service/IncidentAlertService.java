package org.techhive.trackingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.techhive.trackingservice.dto.NotificationResponse;
import org.techhive.trackingservice.entity.DoctorNotification;
import org.techhive.trackingservice.repository.DoctorNotificationRepository;
import org.techhive.trackingservice.repository.MedicalFolderRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class IncidentAlertService {

    private final RestTemplate lbRestTemplate;
    private final RestTemplate plainRestTemplate;
    private final DoctorNotificationRepository notificationRepo;
    private final MedicalFolderRepository medicalFolderRepo;

    @Value("${mailtrap.token}")
    private String mailtrapToken;

    @Value("${mailtrap.inbox-id}")
    private String mailtrapInboxId;

    @Value("${mailtrap.from:noreply@tfakkarni.com}")
    private String fromEmail;

    @Value("${telegram.bot-token:}")
    private String telegramBotToken;

    @Value("${telegram.default-chat-id:}")
    private String telegramChatId;

    @Value("${alert.fallback-email:doctor@gmail.com}")
    private String fallbackEmail;

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
    // MAIN ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

    @Async
    public void handleIncidentAlert(
            String patientKeycloakId, String severity,
            String incidentType, String description,
            String location, String actionTaken,
            String injuryDetails, String occurredAt, String logDate) {

        log.info("══════════════════════════════════════════════════");
        log.info("🚨 ALERT severity={} patient={}", severity, patientKeycloakId);

        // ── ÉTAPE 1: Trouver le médecin ──────────────────────────────────────
        // Stratégie multi-niveaux pour résister aux changements Keycloak
        String doctorKeycloakId = findDoctorId(patientKeycloakId);
        log.info("   doctorKeycloakId résolu: {}", doctorKeycloakId != null ? doctorKeycloakId : "NON TROUVÉ");

        // ── ÉTAPE 2: Récupérer noms + email ──────────────────────────────────
        String patientName = patientKeycloakId;
        String doctorName  = "Docteur";
        String doctorEmail = fallbackEmail;

        try {
            Map<String, String> pInfo = fetchUserInfo(patientKeycloakId);
            if (!pInfo.isEmpty()) {
                patientName = fullName(pInfo, patientKeycloakId);
                log.info("   Patient: {}", patientName);
            }
        } catch (Exception e) {
            log.warn("   fetchUserInfo(patient) KO: {}", e.getMessage());
        }

        if (doctorKeycloakId != null) {
            try {
                Map<String, String> dInfo = fetchUserInfo(doctorKeycloakId);
                if (!dInfo.isEmpty()) {
                    doctorName = fullName(dInfo, "Docteur");
                    String em  = dInfo.getOrDefault("email", "");
                    if (!em.isBlank()) doctorEmail = em;
                }
                log.info("   Doctor: {} email: {}", doctorName, doctorEmail);
            } catch (Exception e) {
                log.warn("   fetchUserInfo(doctor) KO → fallback email={}", fallbackEmail);
            }
        }

        // ── ÉTAPE 3: Sauvegarder la notification ─────────────────────────────
        // TOUJOURS sauvegarder — même si on n'a pas trouvé le médecin
        String finalDoctorId = doctorKeycloakId != null ? doctorKeycloakId : "unknown";
        try {
            DoctorNotification notif = new DoctorNotification();
            notif.setDoctorKeycloakId(finalDoctorId);
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
            log.info("   ✅ Notification sauvegardée en DB — doctorId={}", finalDoctorId);
        } catch (Exception e) {
            log.error("   ❌ Échec save notification: {}", e.getMessage(), e);
        }

        // ── ÉTAPE 4: Email (MODERE + GRAVE) ──────────────────────────────────
        try {
            sendAlertEmail(doctorEmail, doctorName, patientName, severity,
                    incidentType, description, location, injuryDetails, actionTaken, logDate);
            log.info("   ✅ Email envoyé → {}", doctorEmail);
        } catch (Exception e) {
            log.error("   ❌ Email échoué: {}", e.getMessage(), e);
        }

        // ── ÉTAPE 5: Telegram (GRAVE uniquement) ─────────────────────────────
        if ("GRAVE".equalsIgnoreCase(severity)) {
            try {
                sendTelegramAlert(doctorName, patientName, incidentType,
                        description, location, actionTaken, logDate);
            } catch (Exception e) {
                log.error("   ❌ Telegram échoué: {}", e.getMessage(), e);
            }
        }

        log.info("✅ Alerte terminée — severity={}", severity);
        log.info("══════════════════════════════════════════════════");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉSOLUTION DU MÉDECIN — multi-stratégie robuste
    // Résiste aux changements Keycloak (IDs dans medical_folder périmés)
    // ─────────────────────────────────────────────────────────────────────────

    private String findDoctorId(String patientKeycloakId) {

        // Stratégie 1 : chercher dans medical_folders par patientId exact
        try {
            var folders = medicalFolderRepo.findByIdPatient(patientKeycloakId);
            log.info("   [findDoctor] medical_folders par patientId={}: {} résultat(s)", patientKeycloakId, folders.size());
            if (!folders.isEmpty()) {
                String doctorId = folders.get(0).getIdDoctor();
                // Vérifier que cet ID existe encore dans user-service
                Map<String, String> info = fetchUserInfo(doctorId);
                if (!info.isEmpty()) {
                    log.info("   [findDoctor] Stratégie 1 OK → doctorId={}", doctorId);
                    return doctorId;
                }
                // L'ID du dossier est périmé mais on le garde quand même
                log.warn("   [findDoctor] doctorId={} trouvé dans folder mais 404 dans user-service", doctorId);
                return doctorId; // on retourne quand même — le frontend trouvera via smart loading
            }
        } catch (Exception e) {
            log.warn("   [findDoctor] Stratégie 1 KO: {}", e.getMessage());
        }

        // Stratégie 2 : tous les dossiers médicaux (si patient ID a changé dans Keycloak)
        try {
            var allFolders = medicalFolderRepo.findAll();
            log.info("   [findDoctor] Stratégie 2 — total dossiers en DB: {}", allFolders.size());
            if (!allFolders.isEmpty()) {
                // Prendre le premier docteur disponible
                String doctorId = allFolders.get(0).getIdDoctor();
                log.info("   [findDoctor] Stratégie 2 → doctorId={} (premier dossier)", doctorId);
                return doctorId;
            }
        } catch (Exception e) {
            log.warn("   [findDoctor] Stratégie 2 KO: {}", e.getMessage());
        }

        // Stratégie 3 : chercher un médecin dans user-service
        try {
            List<?> doctors = lbRestTemplate.getForObject(
                    "http://user-service/api/users/role/doctor", List.class);
            if (doctors != null && !doctors.isEmpty()) {
                Map<?, ?> first = (Map<?, ?>) doctors.get(0);
                String doctorId = String.valueOf(first.get("keycloakId"));
                log.info("   [findDoctor] Stratégie 3 → doctorId={} (premier médecin user-service)", doctorId);
                return doctorId;
            }
        } catch (Exception e) {
            log.warn("   [findDoctor] Stratégie 3 KO: {}", e.getMessage());
        }

        // Stratégie 4 : utiliser les IDs déjà en DB doctor_notifications
        try {
            List<String> existingIds = notificationRepo.findDistinctDoctorKeycloakIds();
            if (!existingIds.isEmpty()) {
                // Filtrer "unknown" et prendre le premier valide
                String doctorId = existingIds.stream()
                        .filter(id -> !id.equals("unknown") && !id.isBlank())
                        .findFirst()
                        .orElse(null);
                if (doctorId != null) {
                    log.info("   [findDoctor] Stratégie 4 → doctorId={} (depuis notifications existantes)", doctorId);
                    return doctorId;
                }
            }
        } catch (Exception e) {
            log.warn("   [findDoctor] Stratégie 4 KO: {}", e.getMessage());
        }

        log.warn("   [findDoctor] Toutes les stratégies ont échoué — notification sans doctorId");
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NOTIFICATION CRUD
    // ─────────────────────────────────────────────────────────────────────────

    public List<NotificationResponse> getNotificationsForDoctor(String doctorKeycloakId) {
        return notificationRepo
                .findByDoctorKeycloakIdOrderByCreatedAtDesc(doctorKeycloakId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<NotificationResponse> getNotificationsForDoctorIds(List<String> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) return List.of();
        return notificationRepo
                .findByDoctorKeycloakIdInOrderByCreatedAtDesc(doctorIds)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<NotificationResponse> getAllNotifications() {
        return notificationRepo.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<String> getDistinctDoctorIds() {
        return notificationRepo.findDistinctDoctorKeycloakIds();
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
    // TEST
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, String> testAlertDirect(String email) {
        Map<String, String> results = new LinkedHashMap<>();

        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            h.setBearerAuth(mailtrapToken);
            Map<String, Object> body = new HashMap<>();
            body.put("from",    Map.of("email", fromEmail, "name", "Tfakkarni Test"));
            body.put("to",      List.of(Map.of("email", email)));
            body.put("subject", "✅ Test Email — Tfakkarni");
            body.put("html",    "<h2>Test réussi !</h2>");
            body.put("text",    "Test réussi !");
            ResponseEntity<Map> resp = plainRestTemplate.exchange(
                    "https://sandbox.api.mailtrap.io/api/send/" + mailtrapInboxId,
                    HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
            results.put("email", "✅ OK — " + resp.getStatusCode());
        } catch (Exception e) { results.put("email", "❌ " + e.getMessage()); }

        try {
            if (telegramBotToken.isBlank()) {
                results.put("telegram", "⚠️ bot-token vide");
            } else {
                Map<String, Object> body = new HashMap<>();
                body.put("chat_id",    telegramChatId);
                body.put("text",       "✅ <b>Test Tfakkarni</b> — OK !");
                body.put("parse_mode", "HTML");
                HttpHeaders h = new HttpHeaders();
                h.setContentType(MediaType.APPLICATION_JSON);
                ResponseEntity<Map> resp = plainRestTemplate.exchange(
                        "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage",
                        HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
                boolean ok = resp.getBody() != null && Boolean.TRUE.equals(resp.getBody().get("ok"));
                results.put("telegram", ok ? "✅ OK" : "⚠️ " + resp.getBody());
            }
        } catch (Exception e) { results.put("telegram", "❌ " + e.getMessage()); }

        try {
            long total = notificationRepo.count();
            List<String> ids = notificationRepo.findDistinctDoctorKeycloakIds();
            results.put("db_total", String.valueOf(total));
            results.put("db_doctor_ids", String.join(" | ", ids));
        } catch (Exception e) { results.put("db", "❌ " + e.getMessage()); }

        return results;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TELEGRAM
    // ─────────────────────────────────────────────────────────────────────────

    private void sendTelegramAlert(String doctorName, String patientName,
                                   String incidentType, String description,
                                   String location, String actionTaken, String logDate) {
        if (telegramBotToken.isBlank() || telegramChatId.isBlank()) return;

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id",    telegramChatId);
        body.put("text",       buildTelegramMessage(doctorName, patientName, incidentType, description, location, actionTaken, logDate));
        body.put("parse_mode", "HTML");
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> resp = plainRestTemplate.exchange(
                "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage",
                HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
        boolean ok = resp.getBody() != null && Boolean.TRUE.equals(resp.getBody().get("ok"));
        log.info("   {} Telegram → chat_id={}", ok ? "✅" : "⚠️", telegramChatId);
    }

    private String buildTelegramMessage(String doctorName, String patientName,
                                        String incidentType, String description,
                                        String location, String actionTaken, String logDate) {
        String dr = hasValue(doctorName) ? "Dr. " + doctorName : "Docteur";
        String date = logDate;
        try { date = java.time.LocalDate.parse(logDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        catch (Exception ignored) {}
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 <b>ALERTE INCIDENT GRAVE — Tfakkarni</b>\n\n");
        sb.append("Bonjour ").append(esc(dr)).append(",\n\n");
        sb.append("Un incident de gravité <b>GRAVE</b> vient d'être signalé.\n\n");
        sb.append("─────────────────────────\n");
        sb.append("👤 <b>Patient :</b> ").append(esc(patientName)).append("\n");
        sb.append("📅 <b>Date :</b> ").append(esc(date)).append("\n");
        sb.append("🏥 <b>Type :</b> ").append(esc(incidentType)).append("\n");
        if (hasValue(description)) sb.append("📋 <b>Description :</b> ").append(esc(description)).append("\n");
        if (hasValue(location))    sb.append("📍 <b>Lieu :</b> ").append(esc(location)).append("\n");
        if (hasValue(actionTaken)) sb.append("✅ <b>Action prise :</b> ").append(esc(actionTaken)).append("\n");
        sb.append("─────────────────────────\n\n");
        sb.append("🔴 <b>Intervention médicale immédiate requise.</b>\n");
        sb.append("Connectez-vous à la plateforme <b>Tfakkarni</b>.");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMAIL
    // ─────────────────────────────────────────────────────────────────────────

    private void sendAlertEmail(String doctorEmail, String doctorName, String patientName,
                                String severity, String incidentType, String description,
                                String location, String injuryDetails, String actionTaken,
                                String logDate) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(mailtrapToken);
        boolean isGrave = "GRAVE".equalsIgnoreCase(severity);
        Map<String, Object> body = new HashMap<>();
        body.put("from",    Map.of("email", fromEmail, "name", "Tfakkarni Alertes"));
        body.put("to",      List.of(Map.of("email", doctorEmail)));
        body.put("subject", "⚠️ Alerte " + (isGrave ? "GRAVE 🔴" : "MODÉRÉ 🟠") + " — " + patientName);
        body.put("html",    buildEmailHtml(doctorName, patientName, severity, incidentType, description, location, injuryDetails, actionTaken, logDate));
        body.put("text",    "ALERTE " + severity + " | " + patientName + " | " + incidentType);
        plainRestTemplate.exchange("https://sandbox.api.mailtrap.io/api/send/" + mailtrapInboxId,
                HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
    }

    private String buildEmailHtml(String doctorName, String patientName, String severity,
                                  String incidentType, String description, String location,
                                  String injuryDetails, String actionTaken, String logDate) {
        boolean isGrave  = "GRAVE".equalsIgnoreCase(severity);
        String label     = isGrave ? "GRAVE" : "MODÉRÉ";
        String gradStart = isGrave ? "#dc2626" : "#f97316";
        String gradEnd   = isGrave ? "#b91c1c" : "#ea580c";
        String accent    = isGrave ? "#dc2626" : "#f97316";
        String lightBg   = isGrave ? "#fef2f2" : "#fff7ed";
        String border    = isGrave ? "#fca5a5" : "#fed7aa";
        String date = logDate;
        try { date = java.time.LocalDate.parse(logDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        catch (Exception ignored) {}
        StringBuilder extra = new StringBuilder();
        if (hasValue(location))      extra.append(eRow("📍 Lieu",         location,      accent, border));
        if (hasValue(injuryDetails)) extra.append(eRow("🩹 Blessures",    injuryDetails, accent, border));
        if (hasValue(actionTaken))   extra.append(eRow("✅ Action prise", actionTaken,   accent, border));
        String urgency = isGrave
            ? "Un incident de gravité <b>GRAVE</b> a été enregistré. Intervention immédiate requise."
            : "Un incident de gravité <b>MODÉRÉ</b> a été enregistré. Veuillez consulter le dossier.";
        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'></head>"
            + "<body style='margin:0;padding:0;background:#f0f4f8;font-family:Arial,Helvetica,sans-serif;'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f4f8;padding:32px 16px;'><tr><td align='center'>"
            + "<table width='520' cellpadding='0' cellspacing='0' style='background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,.12);'>"
            + "<tr><td style='background:linear-gradient(135deg," + gradStart + "," + gradEnd + ");padding:36px 24px;text-align:center;'>"
            + "<div style='display:inline-block;background:rgba(255,255,255,.2);border-radius:50%;width:64px;height:64px;line-height:64px;font-size:32px;margin-bottom:16px;'>⚠️</div>"
            + "<h1 style='color:#fff;margin:0;font-size:22px;font-weight:800;'>Alerte Incident " + label + "</h1>"
            + "<p style='color:rgba(255,255,255,.85);margin:8px 0 0;font-size:13px;'>Plateforme tfakkarni – Suivi Alzheimer</p>"
            + "</td></tr><tr><td style='padding:32px;'>"
            + "<p style='color:#1f2937;font-size:15px;margin:0 0 8px;'>Bonjour <b>Dr. " + esc(doctorName) + "</b>,</p>"
            + "<p style='color:#6b7280;font-size:14px;line-height:1.7;margin:0 0 24px;'>" + urgency + "</p>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='border:1px solid " + border + ";border-radius:10px;overflow:hidden;background:" + lightBg + ";margin-bottom:20px;'>"
            + eRow("👤 Patient", patientName, accent, border)
            + eRow("🏥 Type", incidentType, accent, border)
            + eRow("📅 Date", date, accent, border)
            + eRow("📋 Description", description, accent, border)
            + extra + "</table>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#fefce8;border:1px solid #fde047;border-radius:10px;'>"
            + "<tr><td style='padding:14px 18px;'><p style='color:#854d0e;font-size:13px;margin:0;'>"
            + "📋 <b>Action requise :</b> Connectez-vous à la plateforme Tfakkarni.</p></td></tr></table>"
            + "</td></tr><tr><td style='background:#f9fafb;border-top:1px solid #e5e7eb;padding:14px 24px;text-align:center;'>"
            + "<p style='color:#9ca3af;font-size:11px;margin:0;'>© 2026 Tfakkarni</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    private String eRow(String label, String value, String accent, String border) {
        return "<tr><td style='padding:11px 16px;border-bottom:1px solid " + border + ";color:" + accent
             + ";font-size:13px;font-weight:700;width:38%;'>" + esc(label) + "</td>"
             + "<td style='padding:11px 16px;border-bottom:1px solid " + border
             + ";color:#111827;font-size:13px;'>" + esc(value) + "</td></tr>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, String> fetchUserInfo(String keycloakId) {
        try {
            Map<?, ?> resp = lbRestTemplate.getForObject(
                    "http://user-service/api/users/keycloak/" + keycloakId, Map.class);
            if (resp != null) {
                Map<String, String> info = new HashMap<>();
                info.put("firstName",  str(resp, "firstName"));
                info.put("lastName",   str(resp, "lastName"));
                info.put("email",      str(resp, "email"));
                info.put("keycloakId", str(resp, "keycloakId"));
                return info;
            }
        } catch (Exception e) {
            log.warn("   fetchUserInfo({}) KO: {}", keycloakId, e.getMessage());
        }
        return new HashMap<>();
    }

    private String str(Map<?, ?> map, String key) {
        Object v = map.get(key); return (v instanceof String s) ? s : "";
    }

    private String fullName(Map<String, String> info, String fallback) {
        String fn = info.getOrDefault("firstName", "").trim();
        String ln = info.getOrDefault("lastName",  "").trim();
        String full = (fn + " " + ln).trim();
        return full.isBlank() ? fallback : full;
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
