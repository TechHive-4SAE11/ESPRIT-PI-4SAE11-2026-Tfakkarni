package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.service.MedicationStatusScheduler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for managing medication status updates.
 * Allows manual triggering of status checks and discontinuing medications.
 */
@RestController
@RequestMapping("/api/admin/medication-status")
@RequiredArgsConstructor
public class MedicationStatusController {

    private final MedicationStatusScheduler medicationStatusScheduler;

    /**
     * Manually trigger a full medication status update
     * 
     * POST /api/admin/medication-status/update
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> triggerStatusUpdate() {
        medicationStatusScheduler.updateAllMedicationStatuses();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Medication status update completed");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Initialize end dates for all medications
     * 
     * POST /api/admin/medication-status/initialize
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeDates() {
        medicationStatusScheduler.initializeMedicationDates();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Medication dates initialized");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Discontinue a specific medication
     * 
     * POST /api/admin/medication-status/{medicationId}/discontinue
     */
    @PostMapping("/{medicationId}/discontinue")
    public ResponseEntity<Map<String, Object>> discontinueMedication(
            @PathVariable Long medicationId,
            @RequestParam(required = false, defaultValue = "Discontinued by doctor") String reason) {
        
        medicationStatusScheduler.discontinueMedication(medicationId, reason);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Medication discontinued");
        response.put("medicationId", medicationId);
        response.put("reason", reason);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get medication status statistics
     * 
     * GET /api/admin/medication-status/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<MedicationStatusScheduler.MedicationStatusStats> getStatistics() {
        return ResponseEntity.ok(medicationStatusScheduler.getStatusStatistics());
    }

    /**
     * Health check endpoint
     * 
     * GET /api/admin/medication-status/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "MedicationStatusScheduler");
        health.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(health);
    }
}
