package org.techhive.trackingservice.mapper;

import org.springframework.stereotype.Component;
import org.techhive.trackingservice.dto.MedicationRequestDTO;
import org.techhive.trackingservice.dto.PrescriptionTemplateResponseDTO;
import org.techhive.trackingservice.dto.TemplateMedicationDTO;
import org.techhive.trackingservice.entity.PrescriptionTemplate;
import org.techhive.trackingservice.entity.TemplateMedication;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PrescriptionTemplateMapper {

    public PrescriptionTemplateResponseDTO toResponseDTO(PrescriptionTemplate template) {
        List<TemplateMedicationDTO> medicationDTOs = new ArrayList<>();
        if (template.getMedications() != null) {
            medicationDTOs = template.getMedications().stream()
                    .map(this::toTemplateMedicationDTO)
                    .collect(Collectors.toList());
        }

        return new PrescriptionTemplateResponseDTO(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getDoctorId(),
                medicationDTOs,
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    public TemplateMedicationDTO toTemplateMedicationDTO(TemplateMedication medication) {
        return new TemplateMedicationDTO(
                medication.getId(),
                medication.getMedicationName(),
                medication.getDosage(),
                medication.getFrequency(),
                medication.getDuration(),
                medication.getInstructions()
        );
    }

    public TemplateMedication toTemplateMedicationEntity(MedicationRequestDTO dto) {
        TemplateMedication medication = new TemplateMedication();
        medication.setMedicationName(dto.getMedicationName());
        medication.setDosage(dto.getDosage());
        medication.setFrequency(dto.getFrequency());
        medication.setDuration(dto.getDuration());
        medication.setInstructions(dto.getInstructions());
        return medication;
    }
}
