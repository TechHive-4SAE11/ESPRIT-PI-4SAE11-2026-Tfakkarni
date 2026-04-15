package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.service.DoctorRatingService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DoctorRatingController {

    private final DoctorRatingService ratingService;

    /**
     * POST /api/ratings — Submit a rating after a meeting
     */
    @PostMapping
    public ResponseEntity<?> submitRating(@RequestBody CreateRatingRequest request) {
        try {
            log.info("Rating request: meetingId={}, doctor={}, patient={}, stars={}",
                    request.getMeetingId(), request.getDoctorKeycloakId(),
                    request.getPatientKeycloakId(), request.getRating());

            DoctorRatingResponse result = ratingService.submitRating(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (IllegalArgumentException e) {
            log.warn("Rating validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("Rating conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            // Log the full stack trace so we can diagnose the 500
            log.error("❌ Error submitting rating: {} — {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Erreur lors de l'enregistrement de l'évaluation.",
                            "cause", e.getClass().getSimpleName() + ": " + e.getMessage()
                    ));
        }
    }

    /**
     * GET /api/ratings/ranking — Doctor podium + ranking for admin
     */
    @GetMapping("/ranking")
    public ResponseEntity<List<DoctorRankingResponse>> getRanking() {
        try {
            return ResponseEntity.ok(ratingService.getDoctorRanking());
        } catch (Exception e) {
            log.error("Error fetching ranking: {}", e.getMessage(), e);
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * GET /api/ratings/doctor/{doctorKeycloakId} — All ratings for a doctor
     */
    @GetMapping("/doctor/{doctorKeycloakId}")
    public ResponseEntity<List<DoctorRatingResponse>> getRatingsForDoctor(
            @PathVariable String doctorKeycloakId) {
        try {
            return ResponseEntity.ok(ratingService.getRatingsForDoctor(doctorKeycloakId));
        } catch (Exception e) {
            log.error("Error fetching doctor ratings: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * GET /api/ratings/check?meetingId=X&patientKeycloakId=Y — Check if already rated
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkRated(
            @RequestParam Long meetingId,
            @RequestParam String patientKeycloakId) {
        try {
            boolean rated = ratingService.hasRated(meetingId, patientKeycloakId);
            return ResponseEntity.ok(Map.of("rated", rated));
        } catch (Exception e) {
            log.error("Error checking rating: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("rated", false));
        }
    }
}
