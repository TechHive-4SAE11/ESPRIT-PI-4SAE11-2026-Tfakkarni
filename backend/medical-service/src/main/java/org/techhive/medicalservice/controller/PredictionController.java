package org.techhive.medicalservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.medicalservice.dto.DashboardStatsDTO;
import org.techhive.medicalservice.dto.PredictionDTO;
import org.techhive.medicalservice.service.PredictionService;
import org.springframework.web.bind.annotation.CrossOrigin;
@RestController
@RequestMapping("/api/medical/predictions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(predictionService.getDashboardStats());
    }

    @GetMapping("/appointment/{id}")
    public ResponseEntity<PredictionDTO> getPredictionForAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(predictionService.predictForAppointment(id));
    }
}
