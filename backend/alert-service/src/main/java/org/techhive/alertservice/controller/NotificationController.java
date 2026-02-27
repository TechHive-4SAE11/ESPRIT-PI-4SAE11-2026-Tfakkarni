package org.techhive.alertservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.alertservice.dto.FcmTokenRequest;
import org.techhive.alertservice.dto.NotificationResponse;
import org.techhive.alertservice.service.MedicationNotificationService;
import org.techhive.alertservice.service.RedisNotificationService;

import java.util.Map;

/**
 * REST API for medication notifications.
 * 
 * Endpoints:
 *   GET    /api/alerts/notifications/{patientId}          — Get today's notifications
 *   POST   /api/alerts/notifications/{patientId}/generate — Generate/refresh notifications
 *   POST   /api/alerts/notifications/{patientId}/refresh  — Force refresh
 *   PATCH  /api/alerts/notifications/{patientId}/{notifId}/read — Mark as read
 *   PATCH  /api/alerts/notifications/{patientId}/read-all — Mark all as read
 *   GET    /api/alerts/notifications/{patientId}/count     — Get unread count
 *   POST   /api/alerts/fcm/register                       — Register FCM token
 *   DELETE /api/alerts/fcm/{patientId}                    — Remove FCM token
 */
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final MedicationNotificationService notificationService;
    private final RedisNotificationService redisService;

    // Constructor for dependency injection
    public NotificationController(MedicationNotificationService notificationService,
                                   RedisNotificationService redisService) {
        this.notificationService = notificationService;
        this.redisService = redisService;
    }

    // ── Notification Endpoints ───────────────────────────────────────────────

    /**
     * Get today's medication notifications for a patient.
     * Generates them if they don't exist yet.
     */
    @GetMapping("/notifications/{patientId}")
    public ResponseEntity<NotificationResponse> getNotifications(@PathVariable String patientId) {
        try {
            NotificationResponse response = notificationService.generateTodayNotifications(patientId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting notifications for patient {}: {}", patientId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Explicitly generate notifications for today (same as GET but more explicit)
     */
    @PostMapping("/notifications/{patientId}/generate")
    public ResponseEntity<NotificationResponse> generateNotifications(@PathVariable String patientId) {
        try {
            NotificationResponse response = notificationService.generateTodayNotifications(patientId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating notifications for patient {}: {}", patientId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Force refresh notifications (delete existing and regenerate)
     */
    @PostMapping("/notifications/{patientId}/refresh")
    public ResponseEntity<NotificationResponse> refreshNotifications(@PathVariable String patientId) {
        try {
            NotificationResponse response = notificationService.refreshNotifications(patientId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error refreshing notifications for patient {}: {}", patientId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Mark a specific notification as read
     */
    @PatchMapping("/notifications/{patientId}/{notificationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable String patientId,
            @PathVariable String notificationId) {
        try {
            boolean success = redisService.markAsRead(patientId, notificationId);
            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Notification marked as read"
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Mark all notifications as read
     */
    @PatchMapping("/notifications/{patientId}/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable String patientId) {
        try {
            int count = redisService.markAllAsRead(patientId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "markedCount", count,
                    "message", count + " notification(s) marked as read"
            ));
        } catch (Exception e) {
            log.error("Error marking all notifications as read: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/notifications/{patientId}/count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable String patientId) {
        try {
            long unread = redisService.getUnreadCount(patientId);
            return ResponseEntity.ok(Map.of(
                    "patientId", patientId,
                    "unreadCount", unread
            ));
        } catch (Exception e) {
            log.error("Error getting unread count for patient {}: {}", patientId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── FCM Token Endpoints ──────────────────────────────────────────────────

    /**
     * Register an FCM token for push notifications
     */
    @PostMapping("/fcm/register")
    public ResponseEntity<Map<String, Object>> registerFcmToken(@RequestBody FcmTokenRequest request) {
        try {
            if (request.getPatientId() == null || request.getFcmToken() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "patientId and fcmToken are required"
                ));
            }
            redisService.saveFcmToken(request.getPatientId(), request.getFcmToken());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "FCM token registered successfully"
            ));
        } catch (Exception e) {
            log.error("Error registering FCM token: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Remove FCM token for a patient
     */
    @DeleteMapping("/fcm/{patientId}")
    public ResponseEntity<Map<String, Object>> removeFcmToken(@PathVariable String patientId) {
        try {
            redisService.removeFcmToken(patientId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "FCM token removed"
            ));
        } catch (Exception e) {
            log.error("Error removing FCM token for patient {}: {}", patientId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "alert-service",
                "module", "medication-notifications"
        ));
    }
}
