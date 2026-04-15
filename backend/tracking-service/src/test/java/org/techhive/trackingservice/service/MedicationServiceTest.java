package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.techhive.trackingservice.dto.MedicationResponseDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.entity.Session;
import org.techhive.trackingservice.entity.MedicalFolder;
import org.techhive.trackingservice.enums.MedicationStatus;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicationServiceTest {

    @Mock
    private MedicationRepository medicationRepository;

    @InjectMocks
    private MedicationService medicationService;

    private Medication medication;
    private Prescription prescription;
    private Session session;

    @BeforeEach
    void setUp() {
        medication = new Medication();
        medication.setId(10L);
        medication.setMedicationName("Doliprane");
        medication.setDosage("1000mg");
        medication.setStatus(MedicationStatus.ACTIVE);

        prescription = new Prescription();
        prescription.setId(100L);

        session = new Session();
        session.setId(1000L);
        session.setSessionDate(LocalDateTime.now());

        MedicalFolder medicalFolder = new MedicalFolder();
        medicalFolder.setIdDoctor("doctor-id");
        session.setMedicalFolder(medicalFolder);

        prescription.setSession(session);
        medication.setPrescription(prescription);
    }

    @Test
    void getMedicationsByPatient_WithStatus_ShouldFilter() {
        Page<Medication> page = new PageImpl<>(Collections.singletonList(medication));
        when(medicationRepository.findByPrescriptionSessionMedicalFolderIdPatientAndStatus(
                eq("patient-id"), eq(MedicationStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        Page<MedicationResponseDTO> result = medicationService.getMedicationsByPatient("patient-id", MedicationStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMedicationName()).isEqualTo("Doliprane");
        verify(medicationRepository).findByPrescriptionSessionMedicalFolderIdPatientAndStatus(any(), any(), any());
    }

    @Test
    void getMedicationsByPatient_WithoutStatus_ShouldGetAll() {
        Page<Medication> page = new PageImpl<>(Collections.singletonList(medication));
        when(medicationRepository.findByPrescriptionSessionMedicalFolderIdPatient(
                eq("patient-id"), any(Pageable.class)))
                .thenReturn(page);

        Page<MedicationResponseDTO> result = medicationService.getMedicationsByPatient("patient-id", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(medicationRepository).findByPrescriptionSessionMedicalFolderIdPatient(any(), any());
    }

    @Test
    void getMedicationsByDoctor_WithStatus_ShouldFilter() {
        Page<Medication> page = new PageImpl<>(Collections.singletonList(medication));
        when(medicationRepository.findByPrescriptionSessionMedicalFolderIdDoctorAndStatus(
                eq("doctor-id"), eq(MedicationStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        Page<MedicationResponseDTO> result = medicationService.getMedicationsByDoctor("doctor-id", MedicationStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        verify(medicationRepository).findByPrescriptionSessionMedicalFolderIdDoctorAndStatus(any(), any(), any());
    }

    @Test
    void updateMedication_ShouldUpdateFieldsAndSave() {
        Medication updateData = new Medication();
        updateData.setMedicationName("Updated Med");
        updateData.setDosage("500mg");

        when(medicationRepository.findById(10L)).thenReturn(Optional.of(medication));
        when(medicationRepository.save(any(Medication.class))).thenReturn(medication);

        MedicationResponseDTO result = medicationService.updateMedication(10L, updateData);

        assertThat(result).isNotNull();
        assertThat(medication.getMedicationName()).isEqualTo("Updated Med");
        assertThat(medication.getDosage()).isEqualTo("500mg");
        verify(medicationRepository).save(medication);
    }

    @Test
    void convertToDTO_ShouldMapAllFields() {
        MedicationResponseDTO dto = medicationService.convertToDTO(medication);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getMedicationName()).isEqualTo("Doliprane");
        assertThat(dto.getSessionId()).isEqualTo(1000L);
        assertThat(dto.getDoctorId()).isEqualTo("doctor-id");
        assertThat(dto.getStatus()).isEqualTo(MedicationStatus.ACTIVE);
    }
}
