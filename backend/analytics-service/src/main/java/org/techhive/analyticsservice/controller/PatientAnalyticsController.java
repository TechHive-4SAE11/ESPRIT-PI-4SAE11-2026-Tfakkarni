package org.techhive.analyticsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.analyticsservice.dto.CognitiveDomainDTO;
import org.techhive.analyticsservice.dto.PatientScoreResponse;
import org.techhive.analyticsservice.entity.ScoreHistory;
import org.techhive.analyticsservice.service.PatientScoreService;

import java.util.List;

@RestController
@RequestMapping("/api/analytics/patient")
@RequiredArgsConstructor
public class PatientAnalyticsController {

    private final PatientScoreService scoreService;

    @GetMapping("/{keycloakId}/score")
    public ResponseEntity<PatientScoreResponse> getPatientScore(@PathVariable String keycloakId) {
        return ResponseEntity.ok(scoreService.getScore(keycloakId));
    }

    @PostMapping("/{keycloakId}/score/recompute")
    public ResponseEntity<PatientScoreResponse> recomputeScore(@PathVariable String keycloakId) {
        return ResponseEntity.ok(scoreService.computeAndSave(keycloakId));
    }

    @GetMapping("/{keycloakId}/score/history")
    public ResponseEntity<List<ScoreHistory>> getScoreHistory(
            @PathVariable String keycloakId,
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(scoreService.getScoreHistory(keycloakId, days));
    }

    @GetMapping("/{keycloakId}/cognitive-domains")
    public ResponseEntity<List<CognitiveDomainDTO>> getCognitiveDomains(@PathVariable String keycloakId) {
        return ResponseEntity.ok(scoreService.computeCognitiveDomains(keycloakId));
    }
}
