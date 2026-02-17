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
                medication.getCreatedAt()
        );
    }
}
