package org.techhive.alertservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.techhive.alertservice.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service that orchestrates:
 *  1. Fetching active medications from the tracking-service
 *  2. Creating notification entries in Redis
 *  3. Triggering Firebase push notifications
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationNotificationService {

    private final WebClient trackingServiceClient;
    private final RedisNotificationService redisService;
    private final FirebasePushService pushService;

    /**
     * Fetch active medications for a patient from the tracking-service,
     * create notification entries in Redis, and optionally send push notifications.
     */
    public NotificationResponse generateTodayNotifications(String patientId) {
        log.info("🔄 Generating today's medication notifications for patient: {}", patientId);

        // 1. Check if notifications already exist in Redis for today
        List<MedicationNotificationDTO> existing = redisService.getNotifications(patientId);
        if (!existing.isEmpty()) {
            log.info("📦 Found {} existing notifications in Redis for patient {}", existing.size(), patientId);
            long unread = existing.stream().filter(n -> !n.isRead()).count();
            return NotificationResponse.builder()
                    .totalNotifications(existing.size())
                    .unreadCount((int) unread)
                    .notifications(existing)
                    .date(LocalDate.now().toString())
                    .message("Medications for today retrieved from cache")
                    .build();
        }

        // 2. Fetch active medications from tracking-service
        List<MedicationDTO> medications = fetchActiveMedications(patientId);

        if (medications.isEmpty()) {
            log.info("ℹ️ No active medications found for patient {}", patientId);
            return NotificationResponse.builder()
                    .totalNotifications(0)
                    .unreadCount(0)
                    .notifications(Collections.emptyList())
                    .date(LocalDate.now().toString())
                    .message("No medications to take today")
                    .build();
        }

        // 3. Create notification entries
        List<MedicationNotificationDTO> notifications = medications.stream()
                .map(med -> MedicationNotificationDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .patientId(patientId)
                        .medicationId(med.getId())
                        .medicationName(med.getMedicationName())
                        .dosage(med.getDosage())
                        .frequency(med.getFrequency())
                        .instructions(med.getInstructions())
                        .status(med.getStatus())
                        .read(false)
                        .pushed(false)
                        .createdAt(LocalDateTime.now())
                        .type("MEDICATION_REMINDER")
                        .build())
                .collect(Collectors.toList());

        // 4. Store in Redis
        redisService.saveNotifications(patientId, notifications);

        // 5. Send push notifications (best-effort)
        try {
            String fcmToken = redisService.getFcmToken(patientId);
            if (fcmToken != null) {
                int sent = pushService.sendBulkMedicationReminders(fcmToken, notifications);
                // Mark pushed ones
                for (int i = 0; i < Math.min(sent, notifications.size()); i++) {
                    notifications.get(i).setPushed(true);
                }
                if (sent > 0) {
                    redisService.saveNotifications(patientId, notifications);
                }
            }
        } catch (Exception e) {
            log.warn("Push notification failed (non-blocking): {}", e.getMessage());
        }

        log.info("✅ Generated {} notifications for patient {}", notifications.size(), patientId);

        return NotificationResponse.builder()
                .totalNotifications(notifications.size())
                .unreadCount(notifications.size()) // All new = unread
                .notifications(notifications)
                .date(LocalDate.now().toString())
                .message("Vous avez " + notifications.size() + " médicament(s) à prendre aujourd'hui")
                .build();
    }

    /**
     * Force-refresh: delete existing and regenerate
     */
    public NotificationResponse refreshNotifications(String patientId) {
        redisService.deleteNotifications(patientId);
        return generateTodayNotifications(patientId);
    }

    /**
     * Get existing notifications (don't regenerate)
     */
    public NotificationResponse getExistingNotifications(String patientId) {
        List<MedicationNotificationDTO> notifications = redisService.getNotifications(patientId);
        long unread = notifications.stream().filter(n -> !n.isRead()).count();

        return NotificationResponse.builder()
                .totalNotifications(notifications.size())
                .unreadCount((int) unread)
                .notifications(notifications)
                .date(LocalDate.now().toString())
                .message(notifications.isEmpty()
                        ? "Aucun médicament pour aujourd'hui"
                        : "Vous avez " + notifications.size() + " médicament(s) à prendre")
                .build();
    }

    /**
     * Fetch active/ongoing medications from tracking-service via REST
     */
    private List<MedicationDTO> fetchActiveMedications(String patientId) {
        try {
            // Fetch ACTIVE medications
            List<MedicationDTO> active = fetchMedicationsByStatus(patientId, "ACTIVE");
            // Fetch ONGOING medications
            List<MedicationDTO> ongoing = fetchMedicationsByStatus(patientId, "ONGOING");

            List<MedicationDTO> allMeds = new ArrayList<>();
            allMeds.addAll(active);
            allMeds.addAll(ongoing);

            log.info("📋 Fetched {} medications from tracking-service for patient {} ({} active, {} ongoing)",
                    allMeds.size(), patientId, active.size(), ongoing.size());
            return allMeds;

        } catch (Exception e) {
            log.error("❌ Failed to fetch medications from tracking-service for patient {}: {}",
                    patientId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<MedicationDTO> fetchMedicationsByStatus(String patientId, String status) {
        try {
            PagedMedicationResponse response = trackingServiceClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/medications/patient/{patientId}/paginated")
                            .queryParam("page", 0)
                            .queryParam("size", 100)
                            .queryParam("status", status)
                            .build(patientId))
                    .retrieve()
                    .bodyToMono(PagedMedicationResponse.class)
                    .block();

            return response != null && response.getContent() != null
                    ? response.getContent()
                    : Collections.emptyList();

        } catch (Exception e) {
            log.warn("Could not fetch {} medications for patient {}: {}", status, patientId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
