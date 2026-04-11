package org.techhive.analyticsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.analyticsservice.dto.FeatureGateResponse;
import org.techhive.analyticsservice.service.FeatureGateService;

@RestController
@RequestMapping("/api/analytics/patient")
@RequiredArgsConstructor
public class FeatureGateController {

    private final FeatureGateService featureGateService;

    @GetMapping("/{keycloakId}/feature-gates")
    public ResponseEntity<FeatureGateResponse> getFeatureGates(@PathVariable String keycloakId) {
        return ResponseEntity.ok(featureGateService.getFeatureGates(keycloakId));
    }

    @PostMapping("/{keycloakId}/feature-gates/recompute")
    public ResponseEntity<FeatureGateResponse> recomputeFeatureGates(@PathVariable String keycloakId) {
        return ResponseEntity.ok(featureGateService.computeAndSave(keycloakId));
    }
}
