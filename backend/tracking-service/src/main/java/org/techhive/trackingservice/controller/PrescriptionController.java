package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.PrescriptionRequestDTO;
import org.techhive.trackingservice.dto.PrescriptionResponseDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.mapper.PrescriptionMapper;
import org.techhive.trackingservice.service.PrescriptionService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PrescriptionMapper prescriptionMapper;

    @PostMapping
    public ResponseEntity<?> createPrescription(@RequestBody PrescriptionRequestDTO requestDTO) {
        try {
            System.out.println("[PrescriptionController] Received request: " + requestDTO);
            System.out.println("[PrescriptionController] SessionId: " + requestDTO.getSessionId());
            System.out.println("[PrescriptionController] Medications count: " + 
                (requestDTO.getMedications() != null ? requestDTO.getMedications().size() : 0));
            
            if (requestDTO.getSessionId() == null) {
                return ResponseEntity.badRequest().body("Session ID is required");
            }
            
            Prescription prescription = new Prescription();
            
            // Convert medication DTOs to entities
            if (requestDTO.getMedications() != null) {
                List<Medication> medications = requestDTO.getMedications().stream()
                        .map(prescriptionMapper::toMedicationEntity)
                        .collect(Collectors.toList());
                prescription.setMedications(medications);
            }
            
            Prescription saved = prescriptionService.createPrescriptionForSession(requestDTO.getSessionId(), prescription);
            return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionMapper.toResponseDTO(saved));
        } catch (RuntimeException e) {
            System.err.println("[PrescriptionController] ERROR creating prescription: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
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
            @RequestBody PrescriptionRequestDTO requestDTO) {
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
