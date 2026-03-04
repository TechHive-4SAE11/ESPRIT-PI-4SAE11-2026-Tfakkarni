package org.techhive.trackingservice.mapper;

import org.springframework.stereotype.Component;
import org.techhive.trackingservice.dto.MedicationRequestDTO;
import org.techhive.trackingservice.dto.MedicationResponseDTO;
import org.techhive.trackingservice.dto.PrescriptionResponseDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PrescriptionMapper {

    public PrescriptionResponseDTO toResponseDTO(Prescription prescription) {
        List<MedicationResponseDTO> medicationDTOs = new ArrayList<>();
        if (prescription.getMedications() != null) {
            medicationDTOs = prescription.getMedications().stream()
                    .map(this::toMedicationResponseDTO)
                    .collect(Collectors.toList());
        }
        
        return new PrescriptionResponseDTO(
                prescription.getId(),
                prescription.getSession().getId(),
                prescription.getSession().getMedicalFolder() != null ? prescription.getSession().getMedicalFolder().getIdDoctor() : null,
                medicationDTOs,
                prescription.getCreatedAt(),
                prescription.getUpdatedAt()
        );
    }

    public Medication toMedicationEntity(MedicationRequestDTO dto) {
        Medication medication = new Medication();
        medication.setMedicationName(dto.getMedicationName());
        medication.setDosage(dto.getDosage());
        medication.setFrequency(dto.getFrequency());
        medication.setDuration(dto.getDuration());
        medication.setInstructions(dto.getInstructions());
        return medication;
    }

    public MedicationResponseDTO toMedicationResponseDTO(Medication medication) {
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
                medication.getPrescription() != null && medication.getPrescription().getSession() != null ?
                    medication.getPrescription().getSession().getId() : null,
                medication.getPrescription() != null && medication.getPrescription().getSession() != null ?
                    medication.getPrescription().getSession().getSessionDate() : null,
                medication.getPrescription() != null && medication.getPrescription().getSession() != null &&
                    medication.getPrescription().getSession().getMedicalFolder() != null ?
                    medication.getPrescription().getSession().getMedicalFolder().getIdDoctor() : null
        );
    }
}
