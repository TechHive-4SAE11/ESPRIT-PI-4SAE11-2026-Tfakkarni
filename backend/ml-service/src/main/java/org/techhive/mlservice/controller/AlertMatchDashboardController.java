package org.techhive.mlservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.mlservice.service.AlertService;
import org.techhive.mlservice.service.ComplianceService;
import org.techhive.mlservice.service.MatchingService;

import java.util.Map;

@RestController
@RequestMapping("/api/ml/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class AlertMatchDashboardController {

    private final AlertService alertService;
    private final ComplianceService complianceService;
    private final MatchingService matchingService;

    @GetMapping("/alerts/{keycloakId}")
    public ResponseEntity<Map<String, Object>> getAlerts(@PathVariable String keycloakId) {
        return ResponseEntity.ok(alertService.getAlerts(keycloakId));
    }

    @GetMapping("/match/{patientId}")
    public ResponseEntity<Map<String, String>> getMatching(@PathVariable String patientId) {
        return ResponseEntity.ok(matchingService.getMatching(patientId));
    }

    @GetMapping("/compliance/{patientId}")
    public ResponseEntity<Map<String, Object>> getCompliance(@PathVariable String patientId) {
        return ResponseEntity.ok(complianceService.calculateCompliance(patientId));
    }
}
