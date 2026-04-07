package org.techhive.medicalservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.techhive.medicalservice.dto.coaching.CoachingNotificationResponse;
import org.techhive.medicalservice.service.coaching.CoachingNotificationService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping({"/api/coaching-notifications", "/api/medical-folders/coaching-notifications"})
@RequiredArgsConstructor
@Slf4j
public class CoachingNotificationController {

    private final CoachingNotificationService coachingNotificationService;
    private final ObjectMapper objectMapper;

    @GetMapping("/my")
    public ResponseEntity<List<CoachingNotificationResponse>> listMyNotifications(
            @RequestParam(name = "recipientUserId", required = false) String recipientUserId,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        String userId = extractKeycloakId(authentication, httpServletRequest);
        String effectiveUserId = StringUtils.hasText(recipientUserId) ? recipientUserId.trim() : userId;
        return ResponseEntity.ok(coachingNotificationService.listMyNotifications(effectiveUserId));
    }

    @GetMapping("/my/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @RequestParam(name = "recipientUserId", required = false) String recipientUserId,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        String userId = extractKeycloakId(authentication, httpServletRequest);
        String effectiveUserId = StringUtils.hasText(recipientUserId) ? recipientUserId.trim() : userId;
        return ResponseEntity.ok(coachingNotificationService.unreadCount(effectiveUserId));
    }

    @PutMapping("/my/{notificationId}/read")
    public ResponseEntity<CoachingNotificationResponse> markRead(
            @PathVariable Long notificationId,
            @RequestParam(name = "recipientUserId", required = false) String recipientUserId,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        String userId = extractKeycloakId(authentication, httpServletRequest);
        String effectiveUserId = StringUtils.hasText(recipientUserId) ? recipientUserId.trim() : userId;
        return ResponseEntity.ok(coachingNotificationService.markRead(effectiveUserId, notificationId));
    }

    @PutMapping("/my/read-all")
    public ResponseEntity<Void> markAllRead(
            @RequestParam(name = "recipientUserId", required = false) String recipientUserId,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        String userId = extractKeycloakId(authentication, httpServletRequest);
        String effectiveUserId = StringUtils.hasText(recipientUserId) ? recipientUserId.trim() : userId;
        coachingNotificationService.markAllRead(effectiveUserId);
        return ResponseEntity.ok().build();
    }

    private String extractKeycloakId(Authentication authentication, HttpServletRequest httpServletRequest) {
        if (authentication != null && authentication.getPrincipal() != null
                && StringUtils.hasText(authentication.getPrincipal().toString())) {
            return authentication.getPrincipal().toString();
        }
        String authHeader = httpServletRequest.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new ResponseStatusException(UNAUTHORIZED, "Invalid JWT token format");
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);
            Object subClaim = claims.get("sub");
            if (subClaim == null || !StringUtils.hasText(subClaim.toString())) {
                throw new ResponseStatusException(UNAUTHORIZED, "JWT token missing subject (sub) claim");
            }
            return subClaim.toString();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to extract subject from bearer token", ex);
            throw new ResponseStatusException(UNAUTHORIZED, "Unable to parse JWT token", ex);
        }
    }
}
