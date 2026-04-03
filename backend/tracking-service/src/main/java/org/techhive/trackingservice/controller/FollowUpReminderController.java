package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.FollowUpReminderResponse;
import org.techhive.trackingservice.service.FollowUpReminderService;

import java.util.List;
import java.util.Map;

/**
 * REST API for follow-up reminders.
 * All routes are protected by the API Gateway (JWT required).
 */
@RestController
@RequestMapping("/api/follow-up-reminders")
@RequiredArgsConstructor
public class FollowUpReminderController {

    private final FollowUpReminderService followUpReminderService;

    /** All reminders for a patient */
    @GetMapping("/patient/{keycloakId}")
    public ResponseEntity<List<FollowUpReminderResponse>> getAll(@PathVariable String keycloakId) {
        return ResponseEntity.ok(followUpReminderService.getReminders(keycloakId));
    }

    /** Unread reminders only */
    @GetMapping("/patient/{keycloakId}/unread")
    public ResponseEntity<List<FollowUpReminderResponse>> getUnread(@PathVariable String keycloakId) {
        return ResponseEntity.ok(followUpReminderService.getUnreadReminders(keycloakId));
    }

    /** Count of unread reminders */
    @GetMapping("/patient/{keycloakId}/count")
    public ResponseEntity<Map<String, Long>> countUnread(@PathVariable String keycloakId) {
        return ResponseEntity.ok(Map.of("count", followUpReminderService.countUnread(keycloakId)));
    }

    /** Mark a single reminder as read */
    @PatchMapping("/{id}/read")
    public ResponseEntity<FollowUpReminderResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(followUpReminderService.markAsRead(id));
    }

    /** Mark all reminders for a patient as read */
    @PatchMapping("/patient/{keycloakId}/read-all")
    public ResponseEntity<Void> markAllAsRead(@PathVariable String keycloakId) {
        followUpReminderService.markAllAsRead(keycloakId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manual trigger for testing — simulates the 22:00 cron job immediately.
     * No need to wait for the scheduled time.
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> triggerCheck() {
        int created = followUpReminderService.checkAndCreateReminders();
        return ResponseEntity.ok(Map.of(
                "message", "Follow-up check completed",
                "remindersCreated", created
        ));
    }
}
