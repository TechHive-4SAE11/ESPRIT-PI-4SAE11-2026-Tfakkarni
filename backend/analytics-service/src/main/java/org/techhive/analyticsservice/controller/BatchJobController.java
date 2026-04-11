package org.techhive.analyticsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.analyticsservice.dto.BatchJobResult;
import org.techhive.analyticsservice.service.ScoreBatchScheduler;

import java.util.List;

@RestController
@RequestMapping("/api/analytics/jobs")
@RequiredArgsConstructor
public class BatchJobController {

    private final ScoreBatchScheduler scheduler;

    @PostMapping("/run-all")
    public ResponseEntity<BatchJobResult> runAll() {
        return ResponseEntity.ok(scheduler.runAllJobs());
    }

    @PostMapping("/patient-scores")
    public ResponseEntity<BatchJobResult> runPatientScores() {
        return ResponseEntity.ok(scheduler.runPatientScores());
    }

    @PostMapping("/doctor-effectiveness")
    public ResponseEntity<BatchJobResult> runDoctorEffectiveness() {
        return ResponseEntity.ok(scheduler.runDoctorEffectiveness());
    }

    @GetMapping("/available")
    public ResponseEntity<List<String>> listAvailableJobs() {
        return ResponseEntity.ok(List.of(
                "run-all",
                "patient-scores",
                "doctor-effectiveness"
        ));
    }
}
