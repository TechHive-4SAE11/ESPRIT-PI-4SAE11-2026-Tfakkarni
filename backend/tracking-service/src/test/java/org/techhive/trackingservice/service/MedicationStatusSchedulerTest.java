package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.entity.Session;
import org.techhive.trackingservice.enums.MedicationStatus;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationStatusSchedulerTest {

    @Mock
    private MedicationRepository medicationRepository;

    private MedicationStatusScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MedicationStatusScheduler(medicationRepository);
    }

    @Test
    void initializeMedicationDates_setsStartAndEndDateFromPrescriptionSession() {
        Medication medication = medication("Amoxicilline", MedicationStatus.ACTIVE, null, null, "10 days");
        medication.setPrescription(prescriptionAt(LocalDateTime.of(2026, 5, 1, 9, 30)));
        when(medicationRepository.findAll()).thenReturn(List.of(medication));

        scheduler.initializeMedicationDates();

        assertThat(medication.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(medication.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 11));
        verify(medicationRepository).saveAll(List.of(medication));
    }

    @Test
    void initializeMedicationDates_doesNotSaveWhenNothingChanged() {
        Medication medication = medication("Vitamine D", MedicationStatus.ACTIVE,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 1), "1 month");
        when(medicationRepository.findAll()).thenReturn(List.of(medication));

        scheduler.initializeMedicationDates();

        verify(medicationRepository, never()).saveAll(anyList());
    }

    @Test
    void updateAllMedicationStatuses_updatesActiveExpiredOngoingAndSkipsDiscontinued() {
        Medication ongoing = medication("Traitement continu", MedicationStatus.ACTIVE,
                LocalDate.now().minusDays(10), null, "ongoing");
        Medication expired = medication("Ancien antibiotique", MedicationStatus.ACTIVE,
                LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), "10 days");
        Medication active = medication("Actif", MedicationStatus.ONGOING,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), "11 days");
        Medication discontinued = medication("Arrêté", MedicationStatus.DISCONTINUED,
                LocalDate.now().minusDays(20), null, "permanent");
        when(medicationRepository.findAll()).thenReturn(List.of(ongoing, expired, active, discontinued));

        scheduler.updateAllMedicationStatuses();

        assertThat(ongoing.getStatus()).isEqualTo(MedicationStatus.ONGOING);
        assertThat(expired.getStatus()).isEqualTo(MedicationStatus.EXPIRED);
        assertThat(active.getStatus()).isEqualTo(MedicationStatus.ACTIVE);
        assertThat(discontinued.getStatus()).isEqualTo(MedicationStatus.DISCONTINUED);
        verify(medicationRepository).saveAll(List.of(ongoing, expired, active, discontinued));
    }

    @Test
    void discontinueMedication_setsStatusEndDateAndAppendsReason() {
        Medication medication = medication("Somnifère", MedicationStatus.ACTIVE,
                LocalDate.now().minusDays(5), null, "30 days");
        medication.setId(42L);
        medication.setInstructions("Take before sleep");
        when(medicationRepository.findById(42L)).thenReturn(Optional.of(medication));

        scheduler.discontinueMedication(42L, "effets secondaires");

        assertThat(medication.getStatus()).isEqualTo(MedicationStatus.DISCONTINUED);
        assertThat(medication.getEndDate()).isEqualTo(LocalDate.now());
        assertThat(medication.getInstructions()).contains("Take before sleep", "Discontinued on", "effets secondaires");
        verify(medicationRepository).save(medication);
    }

    @Test
    void discontinueMedication_throwsWhenMedicationMissing() {
        when(medicationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduler.discontinueMedication(99L, "missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Medication not found: 99");
    }

    @Test
    void getStatusStatistics_countsEachMedicationStatus() {
        when(medicationRepository.findAll()).thenReturn(List.of(
                medication("A", MedicationStatus.ACTIVE, null, null, null),
                medication("B", MedicationStatus.EXPIRED, null, null, null),
                medication("C", MedicationStatus.ONGOING, null, null, null),
                medication("D", MedicationStatus.DISCONTINUED, null, null, null),
                medication("E", MedicationStatus.ACTIVE, null, null, null)
        ));

        MedicationStatusScheduler.MedicationStatusStats stats = scheduler.getStatusStatistics();

        assertThat(stats.total()).isEqualTo(5);
        assertThat(stats.active()).isEqualTo(2);
        assertThat(stats.expired()).isEqualTo(1);
        assertThat(stats.ongoing()).isEqualTo(1);
        assertThat(stats.discontinued()).isEqualTo(1);
    }

    @Test
    void scheduledEntryPointsDelegateToCoreStatusUpdate() {
        Medication medication = medication("A", MedicationStatus.ACTIVE, LocalDate.now(), LocalDate.now().plusDays(1), null);
        when(medicationRepository.findAll()).thenReturn(List.of(medication));

        scheduler.dailyMedicationStatusUpdate();
        scheduler.periodicMedicationStatusUpdate();

        verify(medicationRepository, never()).saveAll(anyList());
    }

    private static Medication medication(String name, MedicationStatus status, LocalDate startDate, LocalDate endDate, String duration) {
        Medication medication = new Medication();
        medication.setMedicationName(name);
        medication.setStatus(status);
        medication.setStartDate(startDate);
        medication.setEndDate(endDate);
        medication.setDuration(duration);
        return medication;
    }

    private static Prescription prescriptionAt(LocalDateTime sessionDate) {
        Session session = new Session();
        session.setSessionDate(sessionDate);
        Prescription prescription = new Prescription();
        prescription.setSession(session);
        return prescription;
    }
}
