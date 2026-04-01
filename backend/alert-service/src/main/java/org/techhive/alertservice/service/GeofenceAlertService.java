package org.techhive.alertservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.techhive.alertservice.dto.GeofenceAlertRequestDTO;
import org.techhive.alertservice.dto.GeofenceAlertResponseDTO;
import org.techhive.alertservice.entity.GeofenceAlert;
import org.techhive.alertservice.repository.GeofenceAlertRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GeofenceAlertService {

  private final GeofenceAlertRepository repository;
  private final RestTemplate restTemplate;

  @Value("${telegram.bot-token:}")
  private String telegramBotToken;

  @Value("${telegram.default-chat-id:}")
  private String telegramChatId;

  @Value("${mailtrap.token:}")
  private String mailtrapToken;

  @Value("${mailtrap.inbox-id:}")
  private String mailtrapInboxId;

  @Value("${mailtrap.from:noreply@tfakkarni.com}")
  private String fromEmail;

  public GeofenceAlertService(GeofenceAlertRepository repository) {
    this.repository = repository;
    this.restTemplate = new RestTemplate();
  }

  public GeofenceAlertResponseDTO reportViolation(GeofenceAlertRequestDTO dto) {
    GeofenceAlert entity = GeofenceAlert.builder()
        .patientId(dto.getPatientId())
        .latitude(dto.getLatitude())
        .longitude(dto.getLongitude())
        .safeZoneName(dto.getSafeZoneName())
        .acknowledged(false)
        .build();

    GeofenceAlert saved = repository.save(entity);
    log.info("🚨 Geofence violation recorded — patient={} lat={} lng={}",
        dto.getPatientId(), dto.getLatitude(), dto.getLongitude());

    // Fire off notifications asynchronously
    sendNotificationsAsync(dto);

    return toResponseDTO(saved);
  }

  public List<GeofenceAlertResponseDTO> getAlerts(String patientId) {
    return repository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
        .map(this::toResponseDTO)
        .collect(Collectors.toList());
  }

  public List<GeofenceAlertResponseDTO> getUnacknowledgedAlerts(String patientId) {
    return repository.findByPatientIdAndAcknowledgedFalse(patientId).stream()
        .map(this::toResponseDTO)
        .collect(Collectors.toList());
  }

  public GeofenceAlertResponseDTO acknowledge(Long id) {
    GeofenceAlert entity = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("GeofenceAlert not found with id: " + id));
    entity.setAcknowledged(true);
    entity.setAcknowledgedAt(LocalDateTime.now());
    return toResponseDTO(repository.save(entity));
  }

  @Async
  protected void sendNotificationsAsync(GeofenceAlertRequestDTO dto) {
    sendTelegramAlert(dto);
    sendEmailAlert(dto);
  }

  private void sendTelegramAlert(GeofenceAlertRequestDTO dto) {
    if (telegramBotToken == null || telegramBotToken.isBlank()
        || telegramChatId == null || telegramChatId.isBlank()) {
      log.warn("Telegram not configured — skipping geofence alert");
      return;
    }

    try {
      String message = "🚨 <b>ALERTE ZONE DE SÉCURITÉ — Tfakkarni</b>\n\n"
          + "Un patient a quitté sa zone de sécurité !\n\n"
          + "─────────────────────────\n"
          + "👤 <b>Patient ID :</b> " + esc(dto.getPatientId()) + "\n"
          + "📍 <b>Position :</b> " + dto.getLatitude() + ", " + dto.getLongitude() + "\n"
          + "🛡️ <b>Zone quittée :</b> " + esc(dto.getSafeZoneName()) + "\n"
          + "📅 <b>Heure :</b> " + LocalDateTime.now().toString() + "\n"
          + "─────────────────────────\n\n"
          + "🔴 <b>Vérifiez immédiatement la localisation du patient.</b>";

      String url = "https://api.telegram.org/bot" + telegramBotToken + "/sendMessage";

      Map<String, Object> body = new HashMap<>();
      body.put("chat_id", telegramChatId);
      body.put("text", message);
      body.put("parse_mode", "HTML");

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
      log.info("✅ Telegram geofence alert sent");
    } catch (Exception e) {
      log.error("❌ Telegram geofence alert failed: {}", e.getMessage());
    }
  }

  private void sendEmailAlert(GeofenceAlertRequestDTO dto) {
    if (mailtrapToken == null || mailtrapToken.isBlank()
        || mailtrapInboxId == null || mailtrapInboxId.isBlank()) {
      log.warn("Mailtrap not configured — skipping geofence email alert");
      return;
    }

    try {
      String emailUrl = "https://sandbox.api.mailtrap.io/api/send/" + mailtrapInboxId;

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setBearerAuth(mailtrapToken);

      String htmlContent = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;'>"
          + "<div style='max-width:500px;margin:0 auto;padding:20px;'>"
          + "<div style='background:linear-gradient(135deg,#dc2626,#b91c1c);color:white;padding:24px;border-radius:12px 12px 0 0;text-align:center;'>"
          + "<h1 style='margin:0;font-size:20px;'>🚨 Alerte Zone de Sécurité</h1>"
          + "<p style='opacity:0.85;margin:8px 0 0;font-size:13px;'>Plateforme Tfakkarni</p>"
          + "</div>"
          + "<div style='background:white;padding:24px;border:1px solid #e5e7eb;border-radius:0 0 12px 12px;'>"
          + "<p>Un patient a quitté sa zone de sécurité.</p>"
          + "<table style='width:100%;border-collapse:collapse;'>"
          + "<tr><td style='padding:8px;font-weight:bold;color:#dc2626;'>Patient ID</td><td style='padding:8px;'>"
          + esc(dto.getPatientId()) + "</td></tr>"
          + "<tr><td style='padding:8px;font-weight:bold;color:#dc2626;'>Position</td><td style='padding:8px;'>"
          + dto.getLatitude() + ", " + dto.getLongitude() + "</td></tr>"
          + "<tr><td style='padding:8px;font-weight:bold;color:#dc2626;'>Zone quittée</td><td style='padding:8px;'>"
          + esc(dto.getSafeZoneName()) + "</td></tr>"
          + "</table>"
          + "<p style='color:#dc2626;font-weight:bold;margin-top:16px;'>⚠️ Vérifiez immédiatement la localisation du patient.</p>"
          + "</div></div></body></html>";

      Map<String, Object> body = new HashMap<>();
      body.put("from", Map.of("email", fromEmail, "name", "Tfakkarni Alertes"));
      body.put("to", List.of(Map.of("email", "doctor@tfakkarni.com")));
      body.put("subject", "🚨 Alerte Zone de Sécurité — Patient hors zone");
      body.put("html", htmlContent);

      restTemplate.exchange(emailUrl, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
      log.info("✅ Email geofence alert sent");
    } catch (Exception e) {
      log.error("❌ Email geofence alert failed: {}", e.getMessage());
    }
  }

  private String esc(String s) {
    if (s == null)
      return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private GeofenceAlertResponseDTO toResponseDTO(GeofenceAlert entity) {
    return GeofenceAlertResponseDTO.builder()
        .id(entity.getId())
        .patientId(entity.getPatientId())
        .latitude(entity.getLatitude())
        .longitude(entity.getLongitude())
        .safeZoneName(entity.getSafeZoneName())
        .acknowledged(entity.isAcknowledged())
        .acknowledgedAt(entity.getAcknowledgedAt())
        .createdAt(entity.getCreatedAt())
        .build();
  }
}
