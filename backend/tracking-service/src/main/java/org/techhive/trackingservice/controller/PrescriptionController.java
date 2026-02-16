package org.techhive.trackingservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.MedicationRequestDTO;
import org.techhive.trackingservice.dto.MedicationResponseDTO;
import org.techhive.trackingservice.dto.PrescriptionRequestDTO;
import org.techhive.trackingservice.dto.PrescriptionResponseDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.service.PrescriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping
    public ResponseEntity<PrescriptionResponseDTO> createPrescription(@RequestBody PrescriptionRequestDTO requestDTO) {
        try {
            Prescription prescription = new Prescription();
            
            // Convert medication DTOs to entities
            if (requestDTO.getMedications() != null) {
                List<Medication> medications = requestDTO.getMedications().stream()
                        .map(this::toMedicationEntity)
                        .collect(Collectors.toList());
                prescription.setMedications(medications);
            }
            
            Prescription saved = prescriptionService.createPrescriptionForSession(requestDTO.getSessionId(), prescription);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(saved));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<PrescriptionResponseDTO>> getAllPrescriptions() {
        List<Prescription> prescriptions = prescriptionService.getAllPrescriptions();
        List<PrescriptionResponseDTO> responseDTOs = prescriptions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponseDTO> getPrescriptionById(@PathVariable Long id) {
        return prescriptionService.getPrescriptionById(id)
                .map(prescription -> ResponseEntity.ok(toResponseDTO(prescription)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsBySession(@PathVariable Long sessionId) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsBySession(sessionId);
        List<PrescriptionResponseDTO> responseDTOs = prescriptions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/patient/{idPatient}")
    public ResponseEntity<List<PrescriptionResponseDTO>> getPrescriptionsByPatient(@PathVariable String idPatient) {
        List<Prescription> prescriptions = prescriptionService.getPrescriptionsByPatient(idPatient);
        List<PrescriptionResponseDTO> responseDTOs = prescriptions.stream()
                .map(this::toResponseDTO)
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
                    .map(this::toMedicationEntity)
                    .collect(Collectors.toList());
            prescription.setMedications(medications);
        }
        
        try {
            Prescription updated = prescriptionService.updatePrescription(id, prescription);
            return ResponseEntity.ok(toResponseDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }

    private PrescriptionResponseDTO toResponseDTO(Prescription prescription) {
        List<MedicationResponseDTO> medicationDTOs = new ArrayList<>();
        if (prescription.getMedications() != null) {
            medicationDTOs = prescription.getMedications().stream()
                    .map(this::toMedicationResponseDTO)
                    .collect(Collectors.toList());
        }
        
        return new PrescriptionResponseDTO(
                prescription.getId(),
                prescription.getSession().getId(),
                medicationDTOs,
                prescription.getCreatedAt(),
                prescription.getUpdatedAt()
        );
    }

    private Medication toMedicationEntity(MedicationRequestDTO dto) {
        Medication medication = new Medication();
        medication.setMedicationName(dto.getMedicationName());
        medication.setDosage(dto.getDosage());
        medication.setFrequency(dto.getFrequency());
        medication.setDuration(dto.getDuration());
        medication.setInstructions(dto.getInstructions());
        return medication;
    }

    private MedicationResponseDTO toMedicationResponseDTO(Medication medication) {
        return new MedicationResponseDTO(
                medication.getId(),
                medication.getMedicationName(),
                medication.getDosage(),
                medication.getFrequency(),
                medication.getDuration(),
                medication.getInstructions(),
                medication.getCreatedAt()
        );
    }
}
