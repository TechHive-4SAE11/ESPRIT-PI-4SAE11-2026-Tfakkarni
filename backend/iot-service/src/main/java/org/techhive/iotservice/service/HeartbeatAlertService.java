package org.techhive.iotservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.techhive.iotservice.client.AlertServiceClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class HeartbeatAlertService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final AlertServiceClient alertServiceClient;

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.default-chat-id:}")
    private String defaultChatId;

    @Value("${heartbeat.alert.high-bpm:120}")
    private int highBpmThreshold;

    @Value("${heartbeat.alert.low-bpm:40}")
    private int lowBpmThreshold;

    @Value("${heartbeat.alert.cooldown-minutes:10}")
    private int cooldownMinutes;

    // Cooldown tracker: patientId -> last alert timestamp
    private final ConcurrentHashMap<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();

    public HeartbeatAlertService(AlertServiceClient alertServiceClient) {
        this.alertServiceClient = alertServiceClient;
    }

    /**
     * Check if BPM is abnormal and send Telegram alert if needed.
     */
    public void checkAndAlert(String patientId, int bpm) {
        if (bpm <= highBpmThreshold && bpm >= lowBpmThreshold) {
            return; // Normal range
        }

        // Check cooldown
        LocalDateTime lastAlert = lastAlertTimes.get(patientId);
        if (lastAlert != null && lastAlert.plusMinutes(cooldownMinutes).isAfter(LocalDateTime.now())) {
            log.debug("Alert cooldown active for patient {}. Skipping.", patientId);
            return;
        }

        String alertType = bpm > highBpmThreshold ? "ELEVATED" : "LOW";
        log.warn("⚠️ Abnormal heartbeat detected! Patient={}, BPM={}, Type={}", patientId, bpm, alertType);

        sendTelegramAlert(patientId, bpm, alertType);
        postIotAlert(patientId, bpm, alertType);
        lastAlertTimes.put(patientId, LocalDateTime.now());
    }

    private void sendTelegramAlert(String patientId, int bpm, String alertType) {
        if (botToken == null || botToken.isBlank() || defaultChatId == null || defaultChatId.isBlank()) {
            log.warn("Telegram not configured — skipping heartbeat alert");
            return;
        }

        try {
            String message = buildAlertMessage(patientId, bpm, alertType);
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", defaultChatId);
            body.put("text", message);
            body.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (Boolean.TRUE.equals(response.getBody() != null ? response.getBody().get("ok") : null)) {
                log.info("✅ Telegram heartbeat alert sent for patient {}", patientId);
            } else {
                log.warn("⚠️ Telegram response: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Telegram heartbeat alert failed: {}", e.getMessage(), e);
        }
    }

    private String buildAlertMessage(String patientId, int bpm, String alertType) {
        String emoji = "ELEVATED".equals(alertType) ? "📈" : "📉";
        String danger = "ELEVATED".equals(alertType) ? "ÉLEVÉ" : "FAIBLE";
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        return "🚨 <b>ALERTE RYTHME CARDIAQUE — Tfakkarni</b>\n\n"
                + emoji + " Rythme cardiaque <b>" + danger + "</b> détecté !\n\n"
                + "─────────────────────────\n"
                + "👤 <b>Patient ID :</b> " + esc(patientId) + "\n"
                + "❤️ <b>BPM :</b> <b>" + bpm + "</b>\n"
                + "⚠️ <b>Type :</b> " + danger + " (seuil: "
                + ("ELEVATED".equals(alertType) ? ">" + highBpmThreshold : "<" + lowBpmThreshold) + ")\n"
                + "📅 <b>Heure :</b> " + now + "\n"
                + "─────────────────────────\n\n"
                + "🔴 <b>Vérifiez immédiatement l'état du patient.</b>\n"
                + "Connectez-vous à la plateforme <b>Tfakkarni</b> pour plus de détails.";
    }

    private void postIotAlert(String patientId, int bpm, String alertType) {
        try {
            String type = "ELEVATED".equals(alertType) ? "ELEVATED_BPM" : "LOW_BPM";
            String message = String.format("Abnormal BPM detected: %d (%s)", bpm, alertType);

            Map<String, Object> request = new HashMap<>();
            request.put("patientId", patientId);
            request.put("alertType", type);
            request.put("value", bpm);
            request.put("message", message);

            alertServiceClient.createIotAlert(request);
            log.info("IoT alert persisted in alert-service for patient {}", patientId);
        } catch (Exception e) {
            log.warn("Failed to persist IoT alert in alert-service: {}", e.getMessage());
        }
    }

    private String esc(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Send an arbitrary HTML message via Telegram bot.
     * Used by HeartbeatAlertService internally and by SleepReportScheduler.
     */
    public void sendTelegramMessage(String htmlMessage) {
        if (botToken == null || botToken.isBlank() || defaultChatId == null || defaultChatId.isBlank()) {
            log.warn("Telegram not configured — skipping message");
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", defaultChatId);
            body.put("text", htmlMessage);
            body.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (Boolean.TRUE.equals(response.getBody() != null ? response.getBody().get("ok") : null)) {
                log.info("✅ Telegram message sent");
            } else {
                log.warn("⚠️ Telegram response: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Telegram message failed: {}", e.getMessage(), e);
        }
    }
}
