package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.enums.MedicationStatus;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for managing individual medications.
 * Allows doctors to update medication status.
 */
@Slf4j
@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MedicationController {

    private final MedicationRepository medicationRepository;

    /**
     * Update medication status
     * 
     * PATCH /api/medications/{medicationId}/status
     * 
     * Body: { "status": "DISCONTINUED", "reason": "Patient allergic reaction" }
     */
    @PatchMapping("/{medicationId}/status")
    public ResponseEntity<?> updateMedicationStatus(
            @PathVariable Long medicationId,
            @RequestBody UpdateStatusRequest request) {
        try {
            Medication medication = medicationRepository.findById(medicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Medication not found with id: " + medicationId));

            MedicationStatus oldStatus = medication.getStatus();
            medication.setStatus(request.getStatus());

            // If discontinuing, set end date to today and add reason to instructions
            if (request.getStatus() == MedicationStatus.DISCONTINUED) {
                medication.setEndDate(LocalDate.now());
                
                if (request.getReason() != null && !request.getReason().isEmpty()) {
                    String updatedInstructions = medication.getInstructions() != null 
                            ? medication.getInstructions() + "\n\n[DISCONTINUED] " + request.getReason()
                            : "[DISCONTINUED] " + request.getReason();
                    medication.setInstructions(updatedInstructions);
                }
            }

            medication.setUpdatedAt(LocalDateTime.now());
            Medication updated = medicationRepository.save(medication);

            log.info("Medication {} status updated: {} → {} by doctor",
                    medicationId, oldStatus, request.getStatus());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("medicationId", medicationId);
            response.put("oldStatus", oldStatus);
            response.put("newStatus", updated.getStatus());
            response.put("endDate", updated.getEndDate());
            response.put("message", "Medication status updated successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error updating medication status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating medication status for id: {}", medicationId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update medication status: " + e.getMessage()));
        }
    }

    /**
     * Get medication details
     * 
     * GET /api/medications/{medicationId}
     */
    @GetMapping("/{medicationId}")
    public ResponseEntity<?> getMedication(@PathVariable Long medicationId) {
        return medicationRepository.findById(medicationId)
                .map(medication -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", medication.getId());
                    response.put("medicationName", medication.getMedicationName());
                    response.put("dosage", medication.getDosage());
                    response.put("frequency", medication.getFrequency());
                    response.put("duration", medication.getDuration());
                    response.put("instructions", medication.getInstructions());
                    response.put("status", medication.getStatus());
                    response.put("startDate", medication.getStartDate());
                    response.put("endDate", medication.getEndDate());
                    response.put("createdAt", medication.getCreatedAt());
                    response.put("updatedAt", medication.getUpdatedAt());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Request DTO for updating medication status
     */
    public static class UpdateStatusRequest {
        private MedicationStatus status;
        private String reason;

        public MedicationStatus getStatus() {
            return status;
        }

        public void setStatus(MedicationStatus status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
