package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.PagedResponse;
import org.techhive.trackingservice.dto.MedicationResponseDTO;
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

    private MedicationResponseDTO convertToDTO(Medication medication) {
        return new MedicationResponseDTO(
            medication.getId(),
            medication.getMedicationName(),
            medication.getDosage(),
            medication.getFrequency(),
            medication.getDuration(),
            medication.getInstructions(),
            medication.getStatus(),
            medication.getStartDate(),
            medication.getEndDate(),
            medication.getCreatedAt(),
            medication.getPrescription() != null && medication.getPrescription().getSession() != null ? medication.getPrescription().getSession().getId() : null,
            medication.getPrescription() != null && medication.getPrescription().getSession() != null ? medication.getPrescription().getSession().getSessionDate() : null,
            medication.getPrescription() != null && medication.getPrescription().getSession() != null && medication.getPrescription().getSession().getMedicalFolder() != null ? medication.getPrescription().getSession().getMedicalFolder().getIdDoctor() : null
        );
    }

    /**
     * Get paginated medications for a patient with optional status filter
     * 
     * GET /api/medications/patient/{idPatient}/paginated?page=0&size=10&sortBy=createdAt&sortDir=DESC&status=ACTIVE
     */
    @GetMapping("/patient/{idPatient}/paginated")
    public ResponseEntity<PagedResponse<MedicationResponseDTO>> getMedicationsByPatientPaginated(
            @PathVariable String idPatient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) MedicationStatus status) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Medication> medicationPage;
            if (status != null) {
                medicationPage = medicationRepository.findByPrescriptionSessionMedicalFolderIdPatientAndStatus(
                        idPatient, status, pageable);
            } else {
                medicationPage = medicationRepository.findByPrescriptionSessionMedicalFolderIdPatient(
                        idPatient, pageable);
            }
            
            PagedResponse<MedicationResponseDTO> response = new PagedResponse<>(
                    medicationPage.getContent().stream().map(this::convertToDTO).toList(),
                    medicationPage.getNumber(),
                    medicationPage.getSize(),
                    medicationPage.getTotalElements(),
                    medicationPage.getTotalPages(),
                    medicationPage.isFirst(),
                    medicationPage.isLast()
            );
            
            log.info("Retrieved {} medications for patient {} (page {}/{})", 
                    response.getContent().size(), idPatient, page, medicationPage.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving medications for patient: {}", idPatient, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get paginated medications for a doctor with optional status filter
     * 
     * GET /api/medications/doctor/{idDoctor}/paginated?page=0&size=10&sortBy=createdAt&sortDir=DESC&status=ACTIVE
     */
    @GetMapping("/doctor/{idDoctor}/paginated")
    public ResponseEntity<PagedResponse<MedicationResponseDTO>> getMedicationsByDoctorPaginated(
            @PathVariable String idDoctor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) MedicationStatus status) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<Medication> medicationPage;
            if (status != null) {
                medicationPage = medicationRepository.findByPrescriptionSessionMedicalFolderIdDoctorAndStatus(
                        idDoctor, status, pageable);
            } else {
                medicationPage = medicationRepository.findByPrescriptionSessionMedicalFolderIdDoctor(
                        idDoctor, pageable);
            }
            
            PagedResponse<MedicationResponseDTO> response = new PagedResponse<>(
                    medicationPage.getContent().stream().map(this::convertToDTO).toList(),
                    medicationPage.getNumber(),
                    medicationPage.getSize(),
                    medicationPage.getTotalElements(),
                    medicationPage.getTotalPages(),
                    medicationPage.isFirst(),
                    medicationPage.isLast()
            );
            
            log.info("Retrieved {} medications for doctor {} (page {}/{})", 
                    response.getContent().size(), idDoctor, page, medicationPage.getTotalPages());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving medications for doctor: {}", idDoctor, e);
            return ResponseEntity.internalServerError().build();
        }
    }

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
     * Update medication details
     * 
     * PUT /api/medications/{medicationId}
     */
    @PutMapping("/{medicationId}")
    public ResponseEntity<?> updateMedication(
            @PathVariable Long medicationId,
            @RequestBody UpdateMedicationRequest request) {
        try {
            Medication medication = medicationRepository.findById(medicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Medication not found with id: " + medicationId));

            medication.setMedicationName(request.getMedicationName());
            medication.setDosage(request.getDosage());
            medication.setFrequency(request.getFrequency());
            medication.setDuration(request.getDuration());
            medication.setInstructions(request.getInstructions());
            medication.setStartDate(request.getStartDate());
            medication.setEndDate(request.getEndDate());
            medication.setUpdatedAt(LocalDateTime.now());

            Medication updated = medicationRepository.save(medication);

            log.info("Medication {} updated by doctor", medicationId);
            return ResponseEntity.ok(this.convertToDTO(updated));
        } catch (IllegalArgumentException e) {
            log.error("Error updating medication: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating medication for id: {}", medicationId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update medication: " + e.getMessage()));
        }
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

    /**
     * Request DTO for updating medication details
     */
    public static class UpdateMedicationRequest {
        private String medicationName;
        private String dosage;
        private String frequency;
        private String duration;
        private String instructions;
        private LocalDate startDate;
        private LocalDate endDate;

        public String getMedicationName() { return medicationName; }
        public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
        
        public String getDosage() { return dosage; }
        public void setDosage(String dosage) { this.dosage = dosage; }
        
        public String getFrequency() { return frequency; }
        public void setFrequency(String frequency) { this.frequency = frequency; }
        
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        
        public String getInstructions() { return instructions; }
        public void setInstructions(String instructions) { this.instructions = instructions; }
        
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }
}
