package org.techhive.alertservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.techhive.alertservice.service.MedicationNotificationService;

import java.util.Set;

/**
 * Scheduled task that periodically generates medication reminders
 * for all patients who have registered FCM tokens.
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

    private static final String FCM_TOKEN_PREFIX = "fcm_tokens:";

    @Scheduled(cron = "${notification.scheduler.medication-reminder-cron:0 0 8,12,18,21 * * *}")
    public void sendMedicationReminders() {
        log.info("⏰ Medication reminder scheduler triggered");

        try {
            // Find all patients who have registered FCM tokens
            Set<String> keys = redisTemplate.keys(FCM_TOKEN_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                log.info("ℹ️ No registered FCM tokens found — skipping");
                return;
            }

            int processed = 0;
            for (String key : keys) {
                String patientId = key.replace(FCM_TOKEN_PREFIX, "");
                try {
                    notificationService.generateTodayNotifications(patientId);
                    processed++;
                } catch (Exception e) {
                    log.error("Failed to process reminders for patient {}: {}", patientId, e.getMessage());
                }
            }

            log.info("✅ Medication reminders processed for {} patients", processed);

        } catch (Exception e) {
            log.error("❌ Medication reminder scheduler failed: {}", e.getMessage(), e);
        }
    }
}
