package org.techhive.iotservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.iotservice.dto.HeartbeatReadingDTO;
import org.techhive.iotservice.dto.SleepAnalysisResponse;
import org.techhive.iotservice.dto.SleepHistoryResponse;
import org.techhive.iotservice.service.FeatureGateClient;
import org.techhive.iotservice.service.IotService;
import org.techhive.iotservice.service.SleepAnalysisService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/iot/heartbeat")
@RequiredArgsConstructor
public class HeartbeatController {

    private final IotService iotService;
    private final SleepAnalysisService sleepAnalysisService;
    private final FeatureGateClient featureGateClient;

    /**
     * Get all heartbeat readings for a patient on a given night.
     * Default: last night (yesterday's date).
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<?> getReadings(
            @PathVariable String patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!featureGateClient.isIotEnabled(patientId)) {
            return iotDisabledResponse();
        }
        if (date == null) {
            date = LocalDate.now().minusDays(1);
        }
        return ResponseEntity.ok(iotService.getHeartbeatReadings(patientId, date));
    }

    /**
     * Get full sleep analysis with stage classification for a patient on a given night.
     */
    @GetMapping("/{patientId}/sleep-analysis")
    public ResponseEntity<?> getSleepAnalysis(
            @PathVariable String patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!featureGateClient.isIotEnabled(patientId)) {
            return iotDisabledResponse();
        }
        if (date == null) {
            date = LocalDate.now().minusDays(1);
        }
        return ResponseEntity.ok(sleepAnalysisService.analyze(patientId, date));
    }

    /**
     * Get sleep history for the last N days (default: 7).
     */
    @GetMapping("/{patientId}/sleep-history")
    public ResponseEntity<?> getSleepHistory(
            @PathVariable String patientId,
            @RequestParam(required = false, defaultValue = "7") int days) {
        if (!featureGateClient.isIotEnabled(patientId)) {
            return iotDisabledResponse();
        }
        return ResponseEntity.ok(sleepAnalysisService.analyzeHistory(patientId, days));
    }

    /**
     * Get the latest heartbeat reading for a patient (for live monitoring).
     */
    @GetMapping("/{patientId}/latest")
    public ResponseEntity<?> getLatestReading(@PathVariable String patientId) {
        if (!featureGateClient.isIotEnabled(patientId)) {
            return iotDisabledResponse();
        }
        HeartbeatReadingDTO latest = iotService.getLatestReading(patientId);
        if (latest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }

    /**
     * Record a new heartbeat reading (for real-time IoT ingestion).
     */
    @PostMapping
    public ResponseEntity<?> recordHeartbeat(@RequestBody HeartbeatReadingDTO dto) {
        if (!featureGateClient.isIotEnabled(dto.getPatientId())) {
            return iotDisabledResponse();
        }
        return ResponseEntity.ok(iotService.recordHeartbeat(dto));
    }

    private ResponseEntity<Map<String, String>> iotDisabledResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "IoT features are not available for this patient's risk level. Only SEVERE stage has IoT access."));
    }
}
