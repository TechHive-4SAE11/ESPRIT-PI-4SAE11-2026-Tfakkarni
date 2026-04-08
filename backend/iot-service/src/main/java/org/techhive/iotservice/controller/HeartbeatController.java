package org.techhive.iotservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.iotservice.dto.HeartbeatReadingDTO;
import org.techhive.iotservice.dto.SleepAnalysisResponse;
import org.techhive.iotservice.service.IotService;
import org.techhive.iotservice.service.SleepAnalysisService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/iot/heartbeat")
@RequiredArgsConstructor
public class HeartbeatController {

    private final IotService iotService;
    private final SleepAnalysisService sleepAnalysisService;

    /**
     * Get all heartbeat readings for a patient on a given night.
     * Default: last night (yesterday's date).
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<List<HeartbeatReadingDTO>> getReadings(
            @PathVariable String patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now().minusDays(1);
        }
        return ResponseEntity.ok(iotService.getHeartbeatReadings(patientId, date));
    }

    /**
     * Get full sleep analysis with stage classification for a patient on a given night.
     */
    @GetMapping("/{patientId}/sleep-analysis")
    public ResponseEntity<SleepAnalysisResponse> getSleepAnalysis(
            @PathVariable String patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) {
            date = LocalDate.now().minusDays(1);
        }
        return ResponseEntity.ok(sleepAnalysisService.analyze(patientId, date));
    }

    /**
     * Get the latest heartbeat reading for a patient (for live monitoring).
     */
    @GetMapping("/{patientId}/latest")
    public ResponseEntity<HeartbeatReadingDTO> getLatestReading(@PathVariable String patientId) {
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
    public ResponseEntity<HeartbeatReadingDTO> recordHeartbeat(@RequestBody HeartbeatReadingDTO dto) {
        return ResponseEntity.ok(iotService.recordHeartbeat(dto));
    }
}
