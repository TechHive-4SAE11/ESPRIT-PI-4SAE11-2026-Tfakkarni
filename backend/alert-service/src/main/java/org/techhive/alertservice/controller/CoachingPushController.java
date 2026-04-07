package org.techhive.alertservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.alertservice.dto.CoachingPushRequest;
import org.techhive.alertservice.service.FirebasePushService;
import org.techhive.alertservice.service.RedisNotificationService;

import java.util.Map;

/**
 * Internal endpoint for medical-service: FCM push for coaching goals.
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts/push")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CoachingPushController {

    private final FirebasePushService firebasePushService;
    private final RedisNotificationService redisNotificationService;

    @PostMapping("/coaching")
    public ResponseEntity<Map<String, Object>> pushCoaching(@RequestBody CoachingPushRequest request) {
        if (request.getPatientId() == null || request.getPatientId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("sent", false, "error", "patientId required"));
        }
        String token = redisNotificationService.getFcmToken(request.getPatientId());
        long goalId = request.getGoalId() != null ? request.getGoalId() : 0L;
        boolean sent = firebasePushService.sendCoachingPush(
                token,
                request.getTitle(),
                request.getBody(),
                goalId,
                request.getNotificationSubType());
        return ResponseEntity.ok(Map.of("sent", sent));
    }
}
