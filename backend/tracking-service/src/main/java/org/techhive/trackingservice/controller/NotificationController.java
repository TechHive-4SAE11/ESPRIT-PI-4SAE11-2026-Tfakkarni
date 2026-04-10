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

    // ── Endpoints normaux ─────────────────────────────────────────────────────

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @PathVariable String doctorId) {
        return ResponseEntity.ok(alertService.getNotificationsForDoctor(doctorId));
    }

    @GetMapping("/doctor/{doctorId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable String doctorId) {
        return ResponseEntity.ok(Map.of("count", alertService.getUnreadCount(doctorId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.markAsRead(id));
    }

    @PutMapping("/doctor/{doctorId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String doctorId) {
        alertService.markAllAsRead(doctorId);
        return ResponseEntity.noContent().build();
    }

    // ── Endpoint multi-IDs ────────────────────────────────────────────────────
    // Utile quand l'ID Keycloak a changé — cherche par plusieurs IDs à la fois
    // GET /api/notifications/by-ids?ids=id1,id2,id3

    @GetMapping("/by-ids")
    public ResponseEntity<List<NotificationResponse>> getByMultipleIds(
            @RequestParam List<String> ids) {
        return ResponseEntity.ok(alertService.getNotificationsForDoctorIds(ids));
    }

    // ── Endpoint debug ────────────────────────────────────────────────────────
    // GET /api/notifications/debug/all — toutes les notifications
    // GET /api/notifications/debug/doctor-ids — tous les doctorKeycloakId en DB

    @GetMapping("/debug/all")
    public ResponseEntity<List<NotificationResponse>> getAllNotifications() {
        return ResponseEntity.ok(alertService.getAllNotifications());
    }

    @GetMapping("/debug/doctor-ids")
    public ResponseEntity<Map<String, Object>> getDistinctDoctorIds() {
        List<String> ids = alertService.getDistinctDoctorIds();
        return ResponseEntity.ok(Map.of(
                "distinctDoctorIds", ids,
                "count", ids.size(),
                "hint", "Ces IDs sont stockés en DB. Comparez avec l'ID du token Keycloak."
        ));
    }

    // ── Endpoint test ─────────────────────────────────────────────────────────
    // GET /api/notifications/test?email=doctor@gmail.com

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testAlerts(
            @RequestParam(defaultValue = "doctor@gmail.com") String email) {
        return ResponseEntity.ok(alertService.testAlertDirect(email));
    }
}
