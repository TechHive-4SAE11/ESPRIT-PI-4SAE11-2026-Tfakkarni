package org.techhive.trackingservice.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.techhive.trackingservice.dto.PagedResponse;
import org.techhive.trackingservice.client.UserServiceClient;
import org.techhive.trackingservice.service.PrescriptionPdfService;
import com.lowagie.text.DocumentException;
import java.io.IOException;
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
    private final PrescriptionPdfService prescriptionPdfService;
    private final UserServiceClient userServiceClient;

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
    
    @GetMapping("/patient/{idPatient}/paginated")
    public ResponseEntity<PagedResponse<PrescriptionResponseDTO>> getPrescriptionsByPatientPaginated(
            @PathVariable String idPatient,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Prescription> prescriptionPage = prescriptionService.getPrescriptionsByPatientPaginated(idPatient, pageable);
        
        List<PrescriptionResponseDTO> responseDTOs = prescriptionPage.getContent().stream()
                .map(prescriptionMapper::toResponseDTO)
                .collect(Collectors.toList());
        
        PagedResponse<PrescriptionResponseDTO> response = PagedResponse.<PrescriptionResponseDTO>builder()
                .content(responseDTOs)
                .page(prescriptionPage.getNumber())
                .size(prescriptionPage.getSize())
                .totalElements(prescriptionPage.getTotalElements())
                .totalPages(prescriptionPage.getTotalPages())
                .first(prescriptionPage.isFirst())
                .last(prescriptionPage.isLast())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePrescription(
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
        } catch (IllegalArgumentException e) {
            log.error("Validation error updating prescription: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getPrescriptionPdf(@PathVariable Long id) {
        if (prescriptionService.getPrescriptionById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Prescription prescription = prescriptionService.getPrescriptionById(id).get();
        try {
            // Resolve doctor signature from user-service (by Neon DB id)
            byte[] signatureImage = null;
            try {
                String doctorId = prescriptionService
                        .getDoctorKeycloakIdForPrescription(id);
                log.info("Resolved doctorId='{}' for prescription #{}", doctorId, id);
                if (doctorId != null && !doctorId.isBlank()) {
                    Long userId = Long.parseLong(doctorId);
                    signatureImage = userServiceClient.getDoctorSignature(userId);
                    log.info("Signature fetch result: {} bytes",
                            signatureImage != null ? signatureImage.length : "null");
                }
            } catch (Exception e) {
                log.warn("Could not fetch doctor signature for prescription #{}: {}", id, e.getMessage());
            }

            byte[] pdfBytes = prescriptionPdfService.generatePrescriptionPdf(prescription, signatureImage);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "prescription_" + id + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (DocumentException | IOException e) {
            log.error("Error generating PDF for prescription {}", id, e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
