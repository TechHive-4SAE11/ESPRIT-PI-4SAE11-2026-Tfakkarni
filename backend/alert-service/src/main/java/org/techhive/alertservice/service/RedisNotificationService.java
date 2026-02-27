package org.techhive.alertservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.techhive.alertservice.dto.MedicationNotificationDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Service for storing and retrieving medication notifications in Redis.
 * 
 * Key structure:
 *   notifications:{patientId}:{date} -> List of MedicationNotificationDTO
 *   fcm_tokens:{patientId} -> FCM token string
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisNotificationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper redisObjectMapper;

    private static final String NOTIFICATION_KEY_PREFIX = "notifications:";
    private static final String FCM_TOKEN_KEY_PREFIX = "fcm_tokens:";
    private static final long NOTIFICATION_TTL_HOURS = 48; // Keep notifications for 48 hours

    // ── Notification CRUD ────────────────────────────────────────────────────

    /**
     * Store today's medication notifications for a patient
     */
    public void saveNotifications(String patientId, List<MedicationNotificationDTO> notifications) {
        String key = buildNotificationKey(patientId, LocalDate.now());
        try {
            redisTemplate.opsForValue().set(key, notifications, NOTIFICATION_TTL_HOURS, TimeUnit.HOURS);
            log.info("💾 Saved {} notifications for patient {} in Redis", notifications.size(), patientId);
        } catch (Exception e) {
            log.error("Failed to save notifications in Redis for patient {}: {}", patientId, e.getMessage());
        }
    }

    /**
     * Get today's medication notifications for a patient
     */
    public List<MedicationNotificationDTO> getNotifications(String patientId) {
        return getNotificationsForDate(patientId, LocalDate.now());
    }

    /**
     * Get medication notifications for a specific date
     */
    public List<MedicationNotificationDTO> getNotificationsForDate(String patientId, LocalDate date) {
        String key = buildNotificationKey(patientId, date);
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw == null) return new ArrayList<>();

            // Deserialize from Redis
            return redisObjectMapper.convertValue(raw, new TypeReference<List<MedicationNotificationDTO>>() {});
        } catch (Exception e) {
            log.error("Failed to get notifications from Redis for patient {}: {}", patientId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Mark a notification as read
     */
    public boolean markAsRead(String patientId, String notificationId) {
        List<MedicationNotificationDTO> notifications = getNotifications(patientId);
        boolean found = false;

        for (MedicationNotificationDTO n : notifications) {
            if (n.getId().equals(notificationId)) {
                n.setRead(true);
                n.setReadAt(LocalDateTime.now());
                found = true;
                break;
            }
        }

        if (found) {
            saveNotifications(patientId, notifications);
            log.info("📖 Marked notification {} as read for patient {}", notificationId, patientId);
        }
        return found;
    }

    /**
     * Mark all notifications as read for a patient
     */
    public int markAllAsRead(String patientId) {
        List<MedicationNotificationDTO> notifications = getNotifications(patientId);
        int count = 0;

        for (MedicationNotificationDTO n : notifications) {
            if (!n.isRead()) {
                n.setRead(true);
                n.setReadAt(LocalDateTime.now());
                count++;
            }
        }

        if (count > 0) {
            saveNotifications(patientId, notifications);
            log.info("📖 Marked {} notifications as read for patient {}", count, patientId);
        }
        return count;
    }

    /**
     * Count unread notifications
     */
    public long getUnreadCount(String patientId) {
        return getNotifications(patientId).stream()
                .filter(n -> !n.isRead())
                .count();
    }

    /**
     * Delete all notifications for a patient (for today)
     */
    public void deleteNotifications(String patientId) {
        String key = buildNotificationKey(patientId, LocalDate.now());
        redisTemplate.delete(key);
        log.info("🗑️ Deleted notifications for patient {}", patientId);
    }

    // ── FCM Token Management ─────────────────────────────────────────────────

    /**
     * Store FCM token for a patient
     */
    public void saveFcmToken(String patientId, String fcmToken) {
        String key = FCM_TOKEN_KEY_PREFIX + patientId;
        redisTemplate.opsForValue().set(key, fcmToken);
        log.info("📱 Saved FCM token for patient {}", patientId);
    }

    /**
     * Get FCM token for a patient
     */
    public String getFcmToken(String patientId) {
        String key = FCM_TOKEN_KEY_PREFIX + patientId;
        Object token = redisTemplate.opsForValue().get(key);
        return token != null ? token.toString() : null;
    }

    /**
     * Remove FCM token for a patient
     */
    public void removeFcmToken(String patientId) {
        String key = FCM_TOKEN_KEY_PREFIX + patientId;
        redisTemplate.delete(key);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildNotificationKey(String patientId, LocalDate date) {
        return NOTIFICATION_KEY_PREFIX + patientId + ":" + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
