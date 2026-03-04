package org.techhive.trackingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.trackingservice.dto.MedicationRequestDTO;
import org.techhive.trackingservice.dto.PrescriptionTemplateRequestDTO;
import org.techhive.trackingservice.dto.PrescriptionTemplateResponseDTO;
import org.techhive.trackingservice.entity.PrescriptionTemplate;
import org.techhive.trackingservice.entity.TemplateMedication;
import org.techhive.trackingservice.mapper.PrescriptionTemplateMapper;
import org.techhive.trackingservice.service.PrescriptionTemplateService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/prescription-templates")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PrescriptionTemplateController {

    private final PrescriptionTemplateService templateService;
    private final PrescriptionTemplateMapper templateMapper;

    @PostMapping
    public ResponseEntity<?> createTemplate(@Valid @RequestBody PrescriptionTemplateRequestDTO requestDTO) {
        try {
            log.info("Creating prescription template '{}' for doctor {}", requestDTO.getName(), requestDTO.getDoctorId());

            PrescriptionTemplate template = new PrescriptionTemplate();
            template.setName(requestDTO.getName());
            template.setDescription(requestDTO.getDescription());
            template.setDoctorId(requestDTO.getDoctorId());

            List<TemplateMedication> medications = requestDTO.getMedications().stream()
                    .map(templateMapper::toTemplateMedicationEntity)
                    .collect(Collectors.toList());
            template.setMedications(medications);

            PrescriptionTemplate saved = templateService.createTemplate(template);
            return ResponseEntity.status(HttpStatus.CREATED).body(templateMapper.toResponseDTO(saved));
        } catch (Exception e) {
            log.error("Error creating template", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create template: " + e.getMessage()));
        }
    }

    @PostMapping("/from-prescription/{prescriptionId}")
    public ResponseEntity<?> createFromPrescription(
            @PathVariable Long prescriptionId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam String doctorId) {
        try {
            log.info("Creating template '{}' from prescription #{}", name, prescriptionId);
            PrescriptionTemplate saved = templateService.createFromPrescription(prescriptionId, name, description, doctorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(templateMapper.toResponseDTO(saved));
        } catch (RuntimeException e) {
            log.error("Error creating template from prescription", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<PrescriptionTemplateResponseDTO>> getTemplatesByDoctor(@PathVariable String doctorId) {
        List<PrescriptionTemplate> templates = templateService.getTemplatesByDoctor(doctorId);
        List<PrescriptionTemplateResponseDTO> dtos = templates.stream()
                .map(templateMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionTemplateResponseDTO> getTemplateById(@PathVariable Long id) {
        return templateService.getTemplateById(id)
                .map(t -> ResponseEntity.ok(templateMapper.toResponseDTO(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/doctor/{doctorId}/search")
    public ResponseEntity<List<PrescriptionTemplateResponseDTO>> searchTemplates(
            @PathVariable String doctorId,
            @RequestParam String query) {
        List<PrescriptionTemplate> templates = templateService.searchTemplates(doctorId, query);
        List<PrescriptionTemplateResponseDTO> dtos = templates.stream()
                .map(templateMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionTemplateRequestDTO requestDTO) {
        try {
            PrescriptionTemplate template = new PrescriptionTemplate();
            template.setName(requestDTO.getName());
            template.setDescription(requestDTO.getDescription());

            List<TemplateMedication> medications = requestDTO.getMedications().stream()
                    .map(templateMapper::toTemplateMedicationEntity)
                    .collect(Collectors.toList());
            template.setMedications(medications);

            PrescriptionTemplate updated = templateService.updateTemplate(id, template);
            return ResponseEntity.ok(templateMapper.toResponseDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
