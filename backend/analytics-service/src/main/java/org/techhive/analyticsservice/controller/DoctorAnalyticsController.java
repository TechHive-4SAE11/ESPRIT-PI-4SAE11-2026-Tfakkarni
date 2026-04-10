package org.techhive.analyticsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.analyticsservice.dto.DoctorEffectivenessResponse;
import org.techhive.analyticsservice.service.DoctorEffectivenessService;

import java.util.List;

@RestController
@RequestMapping("/api/analytics/doctor")
@RequiredArgsConstructor
public class DoctorAnalyticsController {

    private final DoctorEffectivenessService effectivenessService;

    @GetMapping("/{keycloakId}/effectiveness")
    public ResponseEntity<DoctorEffectivenessResponse> getDoctorEffectiveness(
            @PathVariable String keycloakId) {
        return ResponseEntity.ok(effectivenessService.getEffectiveness(keycloakId));
    }

    @PostMapping("/{keycloakId}/effectiveness/recompute")
    public ResponseEntity<DoctorEffectivenessResponse> recompute(@PathVariable String keycloakId) {
        return ResponseEntity.ok(effectivenessService.computeForDoctor(keycloakId));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<DoctorEffectivenessResponse>> getDoctorRanking() {
        return ResponseEntity.ok(effectivenessService.getDoctorRanking());
    }

    @GetMapping("/red-flags")
    public ResponseEntity<List<DoctorEffectivenessResponse>> getRedFlags() {
        return ResponseEntity.ok(effectivenessService.getRedFlags());
    }
}
