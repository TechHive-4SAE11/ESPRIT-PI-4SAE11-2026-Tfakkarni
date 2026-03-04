package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.dto.MedicationRequestDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.entity.PrescriptionTemplate;
import org.techhive.trackingservice.entity.TemplateMedication;
import org.techhive.trackingservice.mapper.PrescriptionTemplateMapper;
import org.techhive.trackingservice.repository.PrescriptionRepository;
import org.techhive.trackingservice.repository.PrescriptionTemplateRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionTemplateService {

    private final PrescriptionTemplateRepository templateRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionTemplateMapper templateMapper;

    /**
     * Create a new prescription template from scratch.
     */
    public PrescriptionTemplate createTemplate(PrescriptionTemplate template) {
        if (template.getMedications() != null) {
            for (TemplateMedication med : template.getMedications()) {
                med.setPrescriptionTemplate(template);
            }
        }
        log.info("Creating prescription template '{}' for doctor {}", template.getName(), template.getDoctorId());
        return templateRepository.save(template);
    }

    /**
     * Create a template from an existing prescription.
     */
    public PrescriptionTemplate createFromPrescription(Long prescriptionId, String name, String description, String doctorId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + prescriptionId));

        PrescriptionTemplate template = new PrescriptionTemplate();
        template.setName(name);
        template.setDescription(description);
        template.setDoctorId(doctorId);

        if (prescription.getMedications() != null) {
            List<TemplateMedication> templateMeds = prescription.getMedications().stream()
                    .map(med -> {
                        TemplateMedication tMed = new TemplateMedication();
                        tMed.setMedicationName(med.getMedicationName());
                        tMed.setDosage(med.getDosage());
                        tMed.setFrequency(med.getFrequency());
                        tMed.setDuration(med.getDuration());
                        tMed.setInstructions(med.getInstructions());
                        tMed.setPrescriptionTemplate(template);
                        return tMed;
                    })
                    .collect(Collectors.toList());
            template.setMedications(templateMeds);
        }

        log.info("Creating template '{}' from prescription #{} for doctor {}", name, prescriptionId, doctorId);
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionTemplate> getTemplatesByDoctor(String doctorId) {
        return templateRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
    }

    @Transactional(readOnly = true)
    public Optional<PrescriptionTemplate> getTemplateById(Long id) {
        return templateRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionTemplate> searchTemplates(String doctorId, String query) {
        return templateRepository.findByDoctorIdAndNameContainingIgnoreCase(doctorId, query);
    }

    public PrescriptionTemplate updateTemplate(Long id, PrescriptionTemplate updated) {
        return templateRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setDescription(updated.getDescription());

                    // Clear and replace medications
                    existing.getMedications().clear();
                    if (updated.getMedications() != null) {
                        for (TemplateMedication med : updated.getMedications()) {
                            med.setPrescriptionTemplate(existing);
                            existing.getMedications().add(med);
                        }
                    }

                    return templateRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
    }

    public void deleteTemplate(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new RuntimeException("Template not found with id: " + id);
        }
        templateRepository.deleteById(id);
    }
}
