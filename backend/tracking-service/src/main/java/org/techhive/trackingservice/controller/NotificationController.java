package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.NotificationResponse;
import org.techhive.trackingservice.service.IncidentAlertService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final IncidentAlertService alertService;

    /** Get all notifications for a doctor (most recent first) */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@PathVariable String doctorId) {
        return ResponseEntity.ok(alertService.getNotificationsForDoctor(doctorId));
    }

    /** Get unread notification count for a doctor */
    @GetMapping("/doctor/{doctorId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String doctorId) {
        return ResponseEntity.ok(Map.of("count", alertService.getUnreadCount(doctorId)));
    }

    /** Mark a single notification as read */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.markAsRead(id));
    }

    /** Mark all notifications as read for a doctor */
    @PutMapping("/doctor/{doctorId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String doctorId) {
        alertService.markAllAsRead(doctorId);
        return ResponseEntity.noContent().build();
    }

    /**
     * TEST ENDPOINT — vérifie que email + Telegram fonctionnent.
     * Appeler avec: GET /api/notifications/test?email=doctor@gmail.com
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testAlerts(
            @RequestParam(defaultValue = "doctor@gmail.com") String email) {
        Map<String, String> results = alertService.testAlertDirect(email);
        return ResponseEntity.ok(results);
    }
}
