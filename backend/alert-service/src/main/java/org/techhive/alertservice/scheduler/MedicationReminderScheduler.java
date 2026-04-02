package org.techhive.alertservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.techhive.alertservice.service.MedicationNotificationService;

import java.util.Map;
import java.util.Set;

/**
 * Scheduled task that periodically generates medication reminders
 * for all patients who have registered FCM tokens.
 *
 * Patients that have been flagged as inactive by the user-service cron job
 * (notificationsEnabled = false) are silently skipped — their account remains
 * fully active, they simply won't receive push reminders until they interact
 * with the platform again.
 *
 * Runs at 8:00, 12:00, 18:00, and 21:00 by default (configurable).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MedicationReminderScheduler {

    private final MedicationNotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;
    @Qualifier("userServiceClient")
    private final WebClient userServiceClient;

    private static final String FCM_TOKEN_PREFIX = "fcm_tokens:";

    @Scheduled(cron = "${notification.scheduler.medication-reminder-cron:0 0 8,12,18,21 * * *}")
    public void sendMedicationReminders() {
        log.info("⏰ Medication reminder scheduler triggered");

        try {
            Set<String> keys = redisTemplate.keys(FCM_TOKEN_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.info("ℹ️ No registered FCM tokens found — skipping");
                return;
            }

            int processed = 0;
            int skipped = 0;
            for (String key : keys) {
                String patientId = key.replace(FCM_TOKEN_PREFIX, "");
                try {
                    if (!isNotificationsEnabled(patientId)) {
                        log.debug("🔕 Skipping inactive patient {}", patientId);
                        skipped++;
                        continue;
                    }
                    notificationService.generateTodayNotifications(patientId);
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to process reminders for patient {}: {}", patientId, e.getMessage());
                }
            }

            log.info("✅ Medication reminders processed for {} patient(s), {} skipped (inactive)", processed, skipped);

        } catch (Exception e) {
            log.error("❌ Medication reminder scheduler failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Calls user-service to check whether a patient should receive notifications.
     * Defaults to {@code true} on any error so reminders are never silently dropped
     * due to a user-service outage.
     */
    @SuppressWarnings("unchecked")
    private boolean isNotificationsEnabled(String patientId) {
        try {
            Map<String, Object> response = userServiceClient.get()
                    .uri("/api/users/{id}/notifications-enabled", patientId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response == null) return true;
            Object enabled = response.get("enabled");
            return enabled == null || Boolean.TRUE.equals(enabled);
        } catch (Exception e) {
            log.warn("Could not check notifications status for patient {} — defaulting to enabled: {}", patientId, e.getMessage());
            return true;
        }
    }
}
