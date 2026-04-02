package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.dto.MedicationResponseDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.enums.MedicationStatus;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medicationRepository;

    @Transactional(readOnly = true)
    public Page<MedicationResponseDTO> getMedicationsByPatient(String idPatient, MedicationStatus status, Pageable pageable) {
        Page<Medication> medicationPage;
        if (status != null) {
            medicationPage = medicationRepository.findByPrescriptionSessionMedicalFolderIdPatientAndStatus(
                    idPatient, status, pageable);
        } else {
            medicationPage = medicationRepository.findByPrescriptionSessionMedicalFolderIdPatient(
                    idPatient, pageable);
        }
        
        return medicationPage.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<MedicationResponseDTO> getMedicationsByDoctor(String idDoctor, MedicationStatus status, Pageable pageable) {
        Page<Medication> medicationPage;
        if (status != null) {
            medicationPage = medicationRepository.findByPrescriptionSessionMedicalFolderIdDoctorAndStatus(
                    idDoctor, status, pageable);
        } else {
            medicationPage = medicationRepository.findByPrescriptionSessionMedicalFolderIdDoctor(
                    idDoctor, pageable);
        }
        
        return medicationPage.map(this::convertToDTO);
    }

    @Transactional
    public MedicationResponseDTO updateMedication(Long id, Medication updatedData) {
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Medication not found with id: " + id));

        medication.setMedicationName(updatedData.getMedicationName());
        medication.setDosage(updatedData.getDosage());
        medication.setFrequency(updatedData.getFrequency());
        medication.setDuration(updatedData.getDuration());
        medication.setInstructions(updatedData.getInstructions());
        medication.setStartDate(updatedData.getStartDate());
        medication.setEndDate(updatedData.getEndDate());
        medication.setUpdatedAt(java.time.LocalDateTime.now());

        Medication saved = medicationRepository.save(medication);
        return convertToDTO(saved);
    }

    public MedicationResponseDTO convertToDTO(Medication medication) {
        // These relations are LAZY but since we are inside a @Transactional method,
        // Hibernate will load them on demand.
        Long sessionId = null;
        java.time.LocalDateTime sessionDate = null;
        String doctorId = null;

        if (medication.getPrescription() != null && medication.getPrescription().getSession() != null) {
            sessionId = medication.getPrescription().getSession().getId();
            sessionDate = medication.getPrescription().getSession().getSessionDate();
            if (medication.getPrescription().getSession().getMedicalFolder() != null) {
                doctorId = medication.getPrescription().getSession().getMedicalFolder().getIdDoctor();
            }
        }

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
            sessionId,
            sessionDate,
            doctorId
        );
    }
}
