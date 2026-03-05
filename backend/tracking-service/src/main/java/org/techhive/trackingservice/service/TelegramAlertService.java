package org.techhive.trackingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Telegram Bot alert service — sends formatted messages on GRAVE incidents.
 *
 * Bot: @tfakkarni_alert_bot
 * API: https://api.telegram.org/bot{TOKEN}/sendMessage
 *
 * Each doctor must start a conversation with the bot once:
 *   1. Search @tfakkarni_alert_bot on Telegram
 *   2. Click START
 *   3. Their chat_id is then used to send alerts
 */
@Slf4j
@Service
public class TelegramAlertService {

    private final RestTemplate plainRestTemplate;

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.default-chat-id:}")
    private String defaultChatId;

    public TelegramAlertService(@Qualifier("plainRestTemplate") RestTemplate plainRestTemplate) {
        this.plainRestTemplate = plainRestTemplate;
    }

    /**
     * Send a Telegram alert message to the doctor.
     *
     * @param chatId       Doctor's Telegram chat ID (stored in their profile), or null to use default
     * @param doctorName   Doctor's full name
     * @param patientName  Patient's full name
     * @param incidentType Type of incident
     * @param description  Incident description
     * @param location     Location (optional)
     * @param actionTaken  Action taken (optional)
     * @param logDate      Date of the log
     */
    @Async
    public void sendGraveAlert(String chatId, String doctorName, String patientName,
                               String incidentType, String description,
                               String location, String actionTaken, String logDate) {

        // Resolve target chat ID
        String targetChatId = (chatId != null && !chatId.isBlank()) ? chatId : defaultChatId;

        if (targetChatId == null || targetChatId.isBlank()) {
            log.warn("⚠️  No Telegram chat ID configured — skipping alert.");
            return;
        }

        log.info("📨 Sending Telegram alert → chat_id={}", targetChatId);

        try {
            String message = buildMessage(doctorName, patientName, incidentType,
                                          description, location, actionTaken, logDate);

            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id",    targetChatId);
            body.put("text",       message);
            body.put("parse_mode", "HTML");  // supports <b>, <i>, <code> tags

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = plainRestTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (Boolean.TRUE.equals(response.getBody() != null
                    ? response.getBody().get("ok") : null)) {
                log.info("✅ Telegram alert sent to chat_id={}", targetChatId);
            } else {
                log.warn("⚠️  Telegram response: {}", response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ Telegram alert failed: {}", e.getMessage(), e);
        }
    }

    private String buildMessage(String doctorName, String patientName,
                                String incidentType, String description,
                                String location, String actionTaken, String logDate) {

        String dr = (doctorName != null && !doctorName.isBlank())
                ? "Dr. " + doctorName : "Docteur";

        StringBuilder sb = new StringBuilder();
        sb.append("🚨 <b>ALERTE INCIDENT GRAVE — Tfakkarni</b>\n\n");
        sb.append("Bonjour ").append(dr).append(",\n\n");
        sb.append("Un incident de gravité <b>GRAVE</b> a été signalé.\n\n");

        sb.append("─────────────────────────\n");
        sb.append("👤 <b>Patient :</b> ").append(esc(patientName)).append("\n");
        sb.append("📅 <b>Date :</b> ").append(esc(logDate)).append("\n");
        sb.append("🏥 <b>Type d'incident :</b> ").append(esc(incidentType)).append("\n");

        if (description != null && !description.isBlank())
            sb.append("📋 <b>Description :</b> ").append(esc(description)).append("\n");

        if (location != null && !location.isBlank())
            sb.append("📍 <b>Lieu :</b> ").append(esc(location)).append("\n");

        if (actionTaken != null && !actionTaken.isBlank())
            sb.append("✅ <b>Action prise :</b> ").append(esc(actionTaken)).append("\n");

        sb.append("─────────────────────────\n\n");
        sb.append("🔴 <b>Intervention médicale immédiate requise.</b>\n");
        sb.append("Connectez-vous à la plateforme <b>Tfakkarni</b> pour consulter le dossier.");

        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
