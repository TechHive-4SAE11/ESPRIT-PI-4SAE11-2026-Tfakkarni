package org.techhive.trackingservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.trackingservice.dto.HealthScoreResponse;
import org.techhive.trackingservice.entity.*;
import org.techhive.trackingservice.repository.DailyLogRepository;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthScoreServiceTest {

    private static final String PATIENT_ID = "patient-123";
    private static final LocalDate DATE = LocalDate.of(2025, 2, 22);

    @Mock
    private DailyLogRepository logRepo;

    @Mock
    private MedicationRepository medicationRepo;

    @InjectMocks
    private HealthScoreService healthScoreService;

    @Nested
    @DisplayName("Règles Hydratation (/9)")
    class HydrationRules {

        @Test
        @DisplayName("Hydratation ≥ 1500 ml → 9/9")
        void hydration_ge_1500_gives_9() {
            DailyLog log = createLogWithHydration(1500);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "HYDRATATION").getScore()).isEqualTo(9);
            assertThat(findBreakdown(r, "HYDRATATION").getMaxScore()).isEqualTo(9);
        }

        @Test
        @DisplayName("Hydratation 1200-1499 ml → 7/9")
        void hydration_1200_to_1499_gives_7() {
            DailyLog log = createLogWithHydration(1200);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "HYDRATATION").getScore()).isEqualTo(7);
            assertThat(findBreakdown(r, "HYDRATATION").getMaxScore()).isEqualTo(9);
        }

        @Test
        @DisplayName("Hydratation 800-1199 ml → 5/9")
        void hydration_800_to_1199_gives_5() {
            DailyLog log = createLogWithHydration(900);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "HYDRATATION").getScore()).isEqualTo(5);
        }

        @Test
        @DisplayName("Hydratation 400-799 ml → 3/9")
        void hydration_400_to_799_gives_3() {
            DailyLog log = createLogWithHydration(400);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "HYDRATATION").getScore()).isEqualTo(3);
            assertThat(findBreakdown(r, "HYDRATATION").getMaxScore()).isEqualTo(9);
        }

        @Test
        @DisplayName("Hydratation < 400 ml → 1/9")
        void hydration_lt_400_gives_1() {
            DailyLog log = createLogWithHydration(200);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "HYDRATATION").getScore()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Règles Médicaments (/75)")
    class MedicationRules {

        @Test
        @DisplayName("100% médicaments pris → 75/75")
        void medications_100_percent_gives_75() {
            DailyLog log = createLogWithMedicationIntakes(3, 3);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID))
                    .thenReturn(List.of(new Medication(), new Medication(), new Medication()));

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "MEDICATIONS").getScore()).isEqualTo(75);
            assertThat(findBreakdown(r, "MEDICATIONS").getMaxScore()).isEqualTo(75);
        }

        @Test
        @DisplayName("0 médicament prescrit → 75/75")
        void zero_expected_medications_gives_75() {
            DailyLog log = createMinimalLog();
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "MEDICATIONS").getScore()).isEqualTo(75);
        }
    }

    @Nested
    @DisplayName("Règles Incidents (/7)")
    class IncidentRules {

        @Test
        @DisplayName("3 incidents ou plus → 0/7")
        void three_or_more_incidents_gives_0() {
            DailyLog log = createLogWithIncidentCount(3);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "INCIDENTS").getScore()).isEqualTo(0);
            assertThat(findBreakdown(r, "INCIDENTS").getMaxScore()).isEqualTo(7);
        }

        @Test
        @DisplayName("0 incident → 7/7")
        void zero_incidents_gives_7() {
            DailyLog log = createMinimalLog();
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "INCIDENTS").getScore()).isEqualTo(7);
        }

        @Test
        @DisplayName("1 incident → 5/7, 2 incidents → 2/7")
        void one_and_two_incidents() {
            DailyLog log1 = createLogWithIncidentCount(1);
            DailyLog log2 = createLogWithIncidentCount(2);
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE))
                    .thenReturn(Optional.of(log1))
                    .thenReturn(Optional.of(log2));

            assertThat(healthScoreService.computeDailyScore(PATIENT_ID, DATE).getBreakdown().stream()
                    .filter(b -> "INCIDENTS".equals(b.getCategory())).findFirst().orElseThrow().getScore()).isEqualTo(5);
            assertThat(healthScoreService.computeDailyScore(PATIENT_ID, DATE).getBreakdown().stream()
                    .filter(b -> "INCIDENTS".equals(b.getCategory())).findFirst().orElseThrow().getScore()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Activité (/9)")
    class ActivityRules {

        @Test
        @DisplayName("Activité ≥ 30 min → 9/9")
        void activity_ge_30_gives_9() {
            DailyLog log = createMinimalLog();
            log.getActivityEntries().clear();
            log.getActivityEntries().add(createPhysicalActivity(30));
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "ACTIVITY").getScore()).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("Aucun log → adjustedMaxScore 100, missingCategories vide")
        void no_log_returns_adjusted_structure() {
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.empty());
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(r.getAdjustedMaxScore()).isEqualTo(100);
            assertThat(r.getMissingCategories()).isEmpty();
            assertThat(r.getTotalScore()).isLessThanOrEqualTo(100);
            assertThat(r.getBreakdown()).hasSize(4);
        }

        @Test
        @DisplayName("Breakdown contient les 4 catégories")
        void breakdown_contains_all_categories() {
            DailyLog log = createMinimalLog();
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            List<String> categories = r.getBreakdown().stream().map(HealthScoreResponse.CategoryBreakdown::getCategory).toList();
            assertThat(categories).containsExactlyInAnyOrder("HYDRATATION", "MEDICATIONS", "ACTIVITY", "INCIDENTS");
        }

        @Test
        @DisplayName("Score parfait = 100 (meds 75 + hydration 9 + activity 9 + incidents 7)")
        void perfect_score_is_100() {
            DailyLog log = createLogWithHydration(1500);
            log.getActivityEntries().clear();
            log.getActivityEntries().add(createPhysicalActivity(30));
            log.getIncidentEntries().clear();
            log.getMedicationIntakes().clear();
            MedicationIntakeLog intake = new MedicationIntakeLog();
            intake.setStatus("PRIS");
            Medication med = new Medication();
            med.setId(1L);
            intake.setMedication(med);
            log.getMedicationIntakes().add(intake);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of(med));

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(r.getTotalScore()).isEqualTo(100);
            assertThat(r.getRiskLevel()).isEqualTo("Excellent");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static HealthScoreResponse.CategoryBreakdown findBreakdown(HealthScoreResponse r, String category) {
        return r.getBreakdown().stream()
                .filter(b -> category.equals(b.getCategory()))
                .findFirst()
                .orElseThrow();
    }

    private DailyLog createMinimalLog() {
        DailyLog log = new DailyLog();
        log.setId(1L);
        log.setPatientKeycloakId(PATIENT_ID);
        log.setLogDate(DATE);
        log.setNutritionEntries(new ArrayList<>(List.of(createNutrition(0))));
        log.setMedicationIntakes(new ArrayList<>());
        log.setActivityEntries(new ArrayList<>(List.of(createPhysicalActivity(5))));
        log.setIncidentEntries(new ArrayList<>());
        return log;
    }

    private DailyLog createLogWithHydration(int totalMl) {
        DailyLog log = createMinimalLog();
        log.getNutritionEntries().clear();
        log.getNutritionEntries().add(createNutrition(totalMl));
        return log;
    }

    private DailyLog createLogWithMedicationIntakes(int totalIntakes, int prisCount) {
        DailyLog log = createMinimalLog();
        log.getMedicationIntakes().clear();
        Medication med = new Medication();
        med.setId(1L);
        for (int i = 0; i < totalIntakes; i++) {
            MedicationIntakeLog mil = new MedicationIntakeLog();
            mil.setDailyLog(log);
            mil.setMedication(med);
            mil.setStatus(i < prisCount ? "PRIS" : "OUBLIE");
            log.getMedicationIntakes().add(mil);
        }
        return log;
    }

    private DailyLog createLogWithIncidentCount(int count) {
        DailyLog log = createMinimalLog();
        log.getIncidentEntries().clear();
        for (int i = 0; i < count; i++) {
            IncidentEntry e = new IncidentEntry();
            e.setDailyLog(log);
            log.getIncidentEntries().add(e);
        }
        return log;
    }

    private static NutritionEntry createNutrition(int hydrationMl) {
        NutritionEntry n = new NutritionEntry();
        n.setHydrationMl(hydrationMl);
        return n;
    }

    private static ActivityEntry createPhysicalActivity(int durationMinutes) {
        ActivityEntry a = new ActivityEntry();
        a.setActivityType("PHYSIQUE");
        a.setDurationMinutes(durationMinutes);
        return a;
    }
}
