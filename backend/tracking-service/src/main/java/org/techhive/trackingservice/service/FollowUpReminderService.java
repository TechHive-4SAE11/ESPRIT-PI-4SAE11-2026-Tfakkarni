package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.techhive.trackingservice.dto.FollowUpReminderResponse;
import org.techhive.trackingservice.entity.DailyLog;
import org.techhive.trackingservice.entity.FollowUpReminder;
import org.techhive.trackingservice.repository.DailyLogRepository;
import org.techhive.trackingservice.repository.FollowUpReminderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FollowUpReminderService {

    private final FollowUpReminderRepository reminderRepository;
    private final DailyLogRepository         dailyLogRepository;
    private final RestTemplate               lbRestTemplate;
    private final RestTemplate               plainRestTemplate;

    @Value("${telegram.bot-token:}")
    private String telegramBotToken;

    @Value("${telegram.default-chat-id:}")
    private String telegramChatId;

    public FollowUpReminderService(
            FollowUpReminderRepository reminderRepository,
            DailyLogRepository dailyLogRepository,
            @Qualifier("lbRestTemplate")    RestTemplate lbRestTemplate,
            @Qualifier("plainRestTemplate") RestTemplate plainRestTemplate) {
        this.reminderRepository = reminderRepository;
        this.dailyLogRepository = dailyLogRepository;
        this.lbRestTemplate     = lbRestTemplate;
        this.plainRestTemplate  = plainRestTemplate;
    }

    @Transactional
    public int checkAndCreateReminders() {
        LocalDate today = LocalDate.now();
        List<String> allPatientIds = reminderRepository.findAllRegisteredPatientIds();

        log.info("══════════════════════════════════════════════════════");
        log.info("[FollowUp] Daily check — {} patients — {}", allPatientIds.size(), today);
        log.info("══════════════════════════════════════════════════════");

        int created = 0;
        List<Map<String, Object>> nonCompliantPatients = new ArrayList<>();

        for (String patientId : allPatientIds) {

            if (reminderRepository.existsByPatientKeycloakIdAndReminderDate(patientId, today)) {
                log.debug("  [skip] Already reminded patient={} today", patientId);
                continue;
            }

            Optional<DailyLog> logOpt = dailyLogRepository.findByPatientKeycloakIdAndLogDate(patientId, today);
            List<String> missing = evaluateCompletion(logOpt.orElse(null));

            if (missing.isEmpty()) {
                log.debug("  [ok] patient={} — suivi complet", patientId);
                continue;
            }

            String patientName = fetchPatientName(patientId);
            log.info("  [alert] Patient='{}' ({}) — manque: {}", patientName, patientId, missing);

            FollowUpReminder reminder = buildReminder(patientId, patientName, today, missing);
            reminderRepository.save(reminder);
            created++;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name",    patientName);
            entry.put("id",      patientId);
            entry.put("missing", missing);
            nonCompliantPatients.add(entry);
        }

        if (!nonCompliantPatients.isEmpty()) {
            sendGroupedTelegramAlert(nonCompliantPatients, today);
        } else {
            log.info("[FollowUp] ✅ Tous les patients ont complété leur suivi aujourd'hui");
        }

        log.info("[FollowUp] Done — {} reminder(s) created", created);
        log.info("══════════════════════════════════════════════════════");
        return created;
    }

    private List<String> evaluateCompletion(DailyLog log) {
        List<String> missing = new ArrayList<>();
        if (log == null) {
            missing.add("NUTRITION");
            missing.add("MEDICATION");
            missing.add("ACTIVITY");
            return missing;
        }
        if (log.getNutritionEntries()  == null || log.getNutritionEntries().isEmpty())  missing.add("NUTRITION");
        if (log.getMedicationIntakes() == null || log.getMedicationIntakes().isEmpty()) missing.add("MEDICATION");
        if (log.getActivityEntries()   == null || log.getActivityEntries().isEmpty())   missing.add("ACTIVITY");
        return missing;
    }

    private FollowUpReminder buildReminder(String patientId, String patientName,
                                           LocalDate date, List<String> missing) {
        String cats   = String.join(",", missing);
        String labels = missing.stream().map(this::categoryLabel).collect(Collectors.joining(", "));
        String msg    = "Le suivi quotidien de " + patientName
                      + " n'a pas été complété. Catégories manquantes : " + labels + ".";
        return FollowUpReminder.builder()
                .patientKeycloakId(patientId)
                .patientName(patientName)
                .reminderDate(date)
                .message(msg)
                .missingCategories(cats)
                .read(false)
                .build();
    }

    private String categoryLabel(String code) {
        return switch (code) {
            case "NUTRITION"  -> "Alimentation";
            case "MEDICATION" -> "Médicaments";
            case "ACTIVITY"   -> "Activités";
            default           -> code;
        };
    }

    @SuppressWarnings("unchecked")
    private String fetchPatientName(String keycloakId) {
        try {
            Map<?, ?> resp = lbRestTemplate.getForObject(
                    "http://user-service/api/users/keycloak/" + keycloakId, Map.class);
            if (resp != null) {
                String first = str(resp, "firstName");
                String last  = str(resp, "lastName");
                String full  = (first + " " + last).trim();
                if (!full.isBlank()) return full;
            }
        } catch (Exception e) {
            log.warn("  fetchPatientName({}) KO: {}", keycloakId, e.getMessage());
        }
        return "Patient " + (keycloakId.length() > 8
                ? keycloakId.substring(0, 8) + "..." : keycloakId);
    }

    private String str(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v instanceof String s ? s : "";
    }

    @SuppressWarnings("unchecked")
    private void sendGroupedTelegramAlert(List<Map<String, Object>> patients, LocalDate date) {
        if (telegramBotToken == null || telegramBotToken.isBlank()
                || telegramChatId == null || telegramChatId.isBlank()) {
            log.warn("[FollowUp] Telegram not configured — skipping");
            return;
        }
        try {
            String url = "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage";
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id",    telegramChatId);
            body.put("parse_mode", "HTML");
            body.put("text",       buildGroupedMessage(patients, date));
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            plainRestTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            log.info("[FollowUp] ✅ Telegram envoyé — {} patient(s)", patients.size());
        } catch (Exception e) {
            log.error("[FollowUp] Telegram échoué: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String buildGroupedMessage(List<Map<String, Object>> patients, LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ <b>RAPPEL SUIVI QUOTIDIEN — Tfakkarni</b>\n");
        sb.append("📅 <b>Date :</b> ").append(dateStr).append("\n\n");
        if (patients.size() == 1) {
            sb.append("Le patient suivant n'a <b>pas complété</b> son suivi aujourd'hui :\n\n");
        } else {
            sb.append("Les ").append(patients.size())
              .append(" patients suivants n'ont <b>pas complété</b> leur suivi aujourd'hui :\n\n");
        }
        for (Map<String, Object> p : patients) {
            String name          = (String) p.get("name");
            List<String> missing = (List<String>) p.get("missing");
            sb.append("👤 <b>").append(esc(name)).append("</b>\n");
            sb.append("   ❌ ");
            String cats = missing.stream().map(c -> switch (c) {
                case "NUTRITION"  -> "🍽️ Alimentation";
                case "MEDICATION" -> "💊 Médicaments";
                case "ACTIVITY"   -> "🏃 Activités";
                default           -> c;
            }).collect(Collectors.joining(" · "));
            sb.append(cats).append("\n\n");
        }
        sb.append("─".repeat(20)).append("\n");
        sb.append("📋 Connectez-vous à <b>Tfakkarni</b> pour compléter le suivi.");
        return sb.toString();
    }

    public List<FollowUpReminderResponse> getReminders(String patientKeycloakId) {
        return reminderRepository
                .findByPatientKeycloakIdOrderByCreatedAtDesc(patientKeycloakId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<FollowUpReminderResponse> getUnreadReminders(String patientKeycloakId) {
        return reminderRepository
                .findByPatientKeycloakIdAndReadFalseOrderByCreatedAtDesc(patientKeycloakId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public long countUnread(String patientKeycloakId) {
        return reminderRepository.countByPatientKeycloakIdAndReadFalse(patientKeycloakId);
    }

    @Transactional
    public FollowUpReminderResponse markAsRead(Long reminderId) {
        FollowUpReminder r = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new RuntimeException("Reminder not found: " + reminderId));
        r.setRead(true);
        r.setReadAt(LocalDateTime.now());
        return toResponse(reminderRepository.save(r));
    }

    @Transactional
    public void markAllAsRead(String patientKeycloakId) {
        List<FollowUpReminder> unread = reminderRepository
                .findByPatientKeycloakIdAndReadFalseOrderByCreatedAtDesc(patientKeycloakId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(r -> { r.setRead(true); r.setReadAt(now); });
        reminderRepository.saveAll(unread);
    }

    private FollowUpReminderResponse toResponse(FollowUpReminder r) {
        return FollowUpReminderResponse.builder()
                .id(r.getId())
                .patientKeycloakId(r.getPatientKeycloakId())
                .patientName(r.getPatientName())
                .reminderDate(r.getReminderDate())
                .message(r.getMessage())
                .missingCategories(r.getMissingCategories())
                .read(r.isRead())
                .readAt(r.getReadAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
