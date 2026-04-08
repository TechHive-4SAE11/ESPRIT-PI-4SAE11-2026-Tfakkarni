package org.techhive.medicalservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.techhive.medicalservice.dto.coaching.CoachingGoalRequest;
import org.techhive.medicalservice.dto.coaching.CoachingGoalResponse;
import org.techhive.medicalservice.dto.coaching.CoachingGoalStatusRequest;
import org.techhive.medicalservice.dto.coaching.CoachingProgressRequest;
import org.techhive.medicalservice.dto.coaching.CoachingProgressResponse;
import org.techhive.medicalservice.service.coaching.CoachingService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medical-folders/{folderId}/coaching-goals")
@RequiredArgsConstructor
@Slf4j
public class CoachingController {

    private final CoachingService coachingService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<CoachingGoalResponse> createGoal(
            @PathVariable Long folderId,
            @Valid @RequestBody CoachingGoalRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        String doctorId = extractKeycloakId(authentication, httpServletRequest);
        CoachingGoalResponse body = coachingService.createGoal(folderId, request, doctorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public ResponseEntity<List<CoachingGoalResponse>> listGoals(@PathVariable Long folderId) {
        return ResponseEntity.ok(coachingService.listGoals(folderId));
    }

    @GetMapping("/{goalId}")
    public ResponseEntity<CoachingGoalResponse> getGoal(@PathVariable Long folderId, @PathVariable Long goalId) {
        return ResponseEntity.ok(coachingService.getGoal(folderId, goalId));
    }

    @PutMapping("/{goalId}")
    public ResponseEntity<CoachingGoalResponse> updateGoal(
            @PathVariable Long folderId,
            @PathVariable Long goalId,
            @Valid @RequestBody CoachingGoalRequest request) {
        return ResponseEntity.ok(coachingService.updateGoal(folderId, goalId, request));
    }

    @PatchMapping("/{goalId}/status")
    public ResponseEntity<CoachingGoalResponse> patchStatus(
            @PathVariable Long folderId,
            @PathVariable Long goalId,
            @Valid @RequestBody CoachingGoalStatusRequest request) {
        return ResponseEntity.ok(coachingService.patchGoalStatus(folderId, goalId, request.getStatus()));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(@PathVariable Long folderId, @PathVariable Long goalId) {
        coachingService.deleteGoal(folderId, goalId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{goalId}/progress")
    public ResponseEntity<CoachingProgressResponse> addProgress(
            @PathVariable Long folderId,
            @PathVariable Long goalId,
            @Valid @RequestBody CoachingProgressRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        String userId = extractKeycloakId(authentication, httpServletRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(coachingService.addProgress(folderId, goalId, request, userId));
    }

    @GetMapping("/{goalId}/progress")
    public ResponseEntity<List<CoachingProgressResponse>> listProgress(
            @PathVariable Long folderId,
            @PathVariable Long goalId) {
        return ResponseEntity.ok(coachingService.listProgress(folderId, goalId));
    }

    private String extractKeycloakId(Authentication authentication, HttpServletRequest httpServletRequest) {
        if (authentication != null && authentication.getPrincipal() != null
                && StringUtils.hasText(authentication.getPrincipal().toString())) {
            return authentication.getPrincipal().toString();
        }

        String authHeader = httpServletRequest.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token format");
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> claims = objectMapper.readValue(payloadJson, Map.class);
            Object subClaim = claims.get("sub");

            if (subClaim == null || !StringUtils.hasText(subClaim.toString())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT token missing subject (sub) claim");
            }

            return subClaim.toString();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to extract subject from bearer token", ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to parse JWT token", ex);
        }
    }
}
