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
import org.techhive.trackingservice.client.MedicamentValidationClient;
import org.techhive.trackingservice.dto.MedicamentValidationResultDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.entity.Session;
import org.techhive.trackingservice.entity.MedicalFolder;
import org.techhive.trackingservice.repository.MedicationIntakeLogRepository;
import org.techhive.trackingservice.repository.PrescriptionRepository;
import org.techhive.trackingservice.repository.SessionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private MedicationIntakeLogRepository medicationIntakeLogRepository;
    @Mock
    private MedicamentValidationClient medicamentValidationClient;

    @InjectMocks
    private PrescriptionService prescriptionService;

    private Prescription prescription;
    private Medication medication;
    private Session session;

    @BeforeEach
    void setUp() {
        prescription = new Prescription();
        prescription.setId(1L);
        prescription.setMedications(new ArrayList<>());

        medication = new Medication();
        medication.setId(1L);
        medication.setMedicationName("Doliprane");
        prescription.getMedications().add(medication);

        session = new Session();
        session.setId(1L);
        session.setSessionDate(LocalDateTime.now());
    }

    @Test
    void createPrescription_WithValidMedications_ShouldSave() {
        MedicamentValidationResultDTO validationResult = new MedicamentValidationResultDTO();
        validationResult.setValid(true);
        when(medicamentValidationClient.validateMedicament(anyString())).thenReturn(validationResult);
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(prescription);

        Prescription created = prescriptionService.createPrescription(prescription);

        assertThat(created).isNotNull();
        verify(prescriptionRepository).save(prescription);
        verify(medicamentValidationClient).validateMedicament("Doliprane");
    }

    @Test
    void createPrescription_WithInvalidMedication_ShouldThrowException() {
        MedicamentValidationResultDTO validationResult = new MedicamentValidationResultDTO();
        validationResult.setValid(false);
        validationResult.setSuggestions(Collections.singletonList("Doliprane 500"));
        when(medicamentValidationClient.validateMedicament(anyString())).thenReturn(validationResult);

        assertThatThrownBy(() -> prescriptionService.createPrescription(prescription))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The following medications are not recognized");

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void createPrescriptionForSession_ShouldLinkAndSave() {
        MedicamentValidationResultDTO validationResult = new MedicamentValidationResultDTO();
        validationResult.setValid(true);
        when(medicamentValidationClient.validateMedicament(anyString())).thenReturn(validationResult);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(prescription);

        Prescription created = prescriptionService.createPrescriptionForSession(1L, prescription);

        assertThat(created).isNotNull();
        assertThat(prescription.getSession()).isEqualTo(session);
        verify(prescriptionRepository).save(prescription);
    }

    @Test
    void getPrescriptionById_ShouldReturnPrescription() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        Optional<Prescription> found = prescriptionService.getPrescriptionById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(1L);
    }

    @Test
    void getDoctorKeycloakIdForPrescription_ShouldReturnId() {
        MedicalFolder medicalFolder = new MedicalFolder();
        medicalFolder.setIdDoctor("doctor-id");
        session.setMedicalFolder(medicalFolder);
        prescription.setSession(session);

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        String doctorId = prescriptionService.getDoctorKeycloakIdForPrescription(1L);

        assertThat(doctorId).isEqualTo("doctor-id");
    }

    @Test
    void deletePrescription_ShouldDeleteAndLogs() {
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        prescriptionService.deletePrescription(1L);

        verify(medicationIntakeLogRepository).deleteByMedicationId(1L);
        verify(prescriptionRepository).delete(prescription);
    }

    @Test
    void getPrescriptionsByPatientPaginated_ShouldReturnPage() {
        Page<Prescription> page = new PageImpl<>(Collections.singletonList(prescription));
        when(prescriptionRepository.findBySessionMedicalFolderIdPatient(eq("patient-id"), any())).thenReturn(page);

        Page<Prescription> result = prescriptionService.getPrescriptionsByPatientPaginated("patient-id", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }
}
