package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.PrescriptionRequestDTO;
import org.techhive.trackingservice.dto.PrescriptionResponseDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.mapper.PrescriptionMapper;
import org.techhive.trackingservice.service.PrescriptionService;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PrescriptionMapper prescriptionMapper;

    @PostMapping
    public ResponseEntity<?> createPrescription(@Valid @RequestBody PrescriptionRequestDTO requestDTO) {
        try {
            log.info("Received prescription creation request: sessionId={}, medicationsCount={}",
                requestDTO.getSessionId(),
                requestDTO.getMedications() != null ? requestDTO.getMedications().size() : 0);
            
            Prescription prescription = new Prescription();
            
            // Convert medication DTOs to entities
            List<Medication> medications = requestDTO.getMedications().stream()
                    .map(prescriptionMapper::toMedicationEntity)
                    .collect(Collectors.toList());
            prescription.setMedications(medications);

            Prescription saved = prescriptionService.createPrescriptionForSession(requestDTO.getSessionId(), prescription);
            log.info("Prescription created successfully with ID: {}", saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionMapper.toResponseDTO(saved));
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating prescription: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error creating prescription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create prescription: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<PrescriptionResponseDTO>> getAllPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getAllPrescriptions();
        List<PrescriptionResponseDTO> responseDTOs = prescriptions.stream()
                .map(prescriptionMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponseDTO> getPrescriptionById(@PathVariable Long id) {
        return prescriptionService.getPrescriptionById(id)
                .map(prescription -> ResponseEntity.ok(prescriptionMapper.toResponseDTO(prescription)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsBySession(@PathVariable Long sessionId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsBySession(sessionId);
        List<PrescriptionResponseDTO> responseDTOs = prescriptions.stream()
                .map(prescriptionMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/patient/{idPatient}")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsByPatient(@PathVariable String idPatient) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPatient(idPatient);
        List<PrescriptionResponseDTO> responseDTOs = prescriptions.stream()
                .map(prescriptionMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionResponseDTO> updatePrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionRequestDTO requestDTO) {
        Prescription prescription = new Prescription();
        
        // Convert medication DTOs to entities
        if (requestDTO.getMedications() != null) {
            List<Medication> medications = requestDTO.getMedications().stream()
                    .map(prescriptionMapper::toMedicationEntity)
                    .collect(Collectors.toList());
            prescription.setMedications(medications);
        }
        
        try {
            Prescription updated = prescriptionService.updatePrescription(id, prescription);
            return ResponseEntity.ok(prescriptionMapper.toResponseDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }
}
