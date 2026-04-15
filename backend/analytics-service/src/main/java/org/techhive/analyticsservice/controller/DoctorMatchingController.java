package org.techhive.analyticsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.analyticsservice.dto.DoctorMatchResponse;
import org.techhive.analyticsservice.dto.SeverePatientResponse;
import org.techhive.analyticsservice.entity.AlzheimerStage;
import org.techhive.analyticsservice.service.DoctorMatchingService;

import java.util.List;

@RestController
@RequestMapping("/api/analytics/matching")
@RequiredArgsConstructor
public class DoctorMatchingController {

    private final DoctorMatchingService matchingService;

    /**
     * GET /api/analytics/matching/ranked-doctors
     * Returns all doctors ranked by composite matching score.
     */
    @GetMapping("/ranked-doctors")
    public ResponseEntity<List<DoctorMatchResponse>> getRankedDoctors() {
        return ResponseEntity.ok(matchingService.getRankedDoctors());
    }

    /**
     * GET /api/analytics/matching/recommend?stage=SEVERE
     * Returns the single best doctor recommendation for a given patient stage.
     */
    @GetMapping("/recommend")
    public ResponseEntity<DoctorMatchResponse> recommendDoctor(
            @RequestParam(defaultValue = "SEVERE") AlzheimerStage stage) {
        return matchingService.recommendDoctor(stage)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * GET /api/analytics/matching/severe-patients
     * Returns all SEVERE + MODERATE patients with their current doctor
     * and a recommended better doctor.
     */
    @GetMapping("/severe-patients")
    public ResponseEntity<List<SeverePatientResponse>> getSeverePatients() {
        return ResponseEntity.ok(matchingService.getSeverePatientsWithRecommendations());
    }
}
