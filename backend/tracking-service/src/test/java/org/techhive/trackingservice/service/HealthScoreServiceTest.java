package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
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
    @DisplayName("Règles Hydratation")
    class HydrationRules {

        @Test
        @DisplayName("Hydratation ≥ 1500 ml → 20/20")
        void hydration_ge_1500_gives_20() {
            DailyLog log = createLogWithHydration(1500);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(r.getTotalScore()).isGreaterThanOrEqualTo(20);
            assertThat(findBreakdown(r, "HYDRATATION").getScore()).isEqualTo(20);
            assertThat(findBreakdown(r, "HYDRATATION").getMaxScore()).isEqualTo(20);
        }

        @Test
        @DisplayName("Hydratation 1000-1499 ml → 15/20")
        void hydration_1000_to_1499_gives_15() {
            DailyLog log = createLogWithHydration(1200);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "HYDRATATION").getScore()).isEqualTo(15);
            assertThat(findBreakdown(r, "HYDRATATION").getMaxScore()).isEqualTo(20);
        }

        @Test
        @DisplayName("Hydratation 500-999 ml → 8/20")
        void hydration_500_to_999_gives_8() {
            DailyLog log = createLogWithHydration(700);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "HYDRATATION").getScore()).isEqualTo(8);
        }

        @Test
        @DisplayName("Hydratation < 500 ml → 3/20")
        void hydration_lt_500_gives_3() {
            DailyLog log = createLogWithHydration(400);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "HYDRATATION").getScore()).isEqualTo(3);
            assertThat(findBreakdown(r, "HYDRATATION").getMaxScore()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Règles Médicaments")
    class MedicationRules {

        @Test
        @DisplayName("100% médicaments pris → 25/25")
        void medications_100_percent_gives_25() {
            DailyLog log = createLogWithMedicationIntakes(3, 3); // 3 pris sur 3 attendus
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID))
                    .thenReturn(List.of(new Medication(), new Medication(), new Medication()));

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "MEDICATIONS").getScore()).isEqualTo(25);
            assertThat(findBreakdown(r, "MEDICATIONS").getMaxScore()).isEqualTo(25);
        }

        @Test
        @DisplayName("0 médicament prescrit → 25/25")
        void zero_expected_medications_gives_25() {
            DailyLog log = createMinimalLog();
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "MEDICATIONS").getScore()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("Règles Incidents")
    class IncidentRules {

        @Test
        @DisplayName("3 incidents ou plus → 0/15")
        void three_or_more_incidents_gives_0() {
            DailyLog log = createLogWithIncidentCount(3);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "INCIDENTS").getScore()).isEqualTo(0);
            assertThat(findBreakdown(r, "INCIDENTS").getMaxScore()).isEqualTo(15);
        }

        @Test
        @DisplayName("0 incident → 15/15")
        void zero_incidents_gives_15() {
            DailyLog log = createMinimalLog();
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "INCIDENTS").getScore()).isEqualTo(15);
        }

        @Test
        @DisplayName("1 incident → 10/15, 2 incidents → 5/15")
        void one_and_two_incidents() {
            DailyLog log1 = createLogWithIncidentCount(1);
            DailyLog log2 = createLogWithIncidentCount(2);
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE))
                    .thenReturn(Optional.of(log1))
                    .thenReturn(Optional.of(log2));

            assertThat(healthScoreService.computeDailyScore(PATIENT_ID, DATE).getBreakdown().stream()
                    .filter(b -> "INCIDENTS".equals(b.getCategory())).findFirst().orElseThrow().getScore()).isEqualTo(10);
            assertThat(healthScoreService.computeDailyScore(PATIENT_ID, DATE).getBreakdown().stream()
                    .filter(b -> "INCIDENTS".equals(b.getCategory())).findFirst().orElseThrow().getScore()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Données manquantes - exclusion proportionnelle")
    class MissingDataExclusion {

        @Test
        @DisplayName("Sommeil absent → SLEEP dans missingCategories et exclusion du calcul")
        void sleep_absent_excluded_from_calculation() {
            DailyLog log = createMinimalLog();
            log.setSleepHours(null);
            log.setMoodLevel("BONNE");
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(r.getMissingCategories()).containsExactly("SLEEP");
            assertThat(findBreakdown(r, "SLEEP").isExcluded()).isTrue();
            assertThat(findBreakdown(r, "SLEEP").getRawValue()).isEqualTo("Donnée non renseignée");
            assertThat(r.getAdjustedMaxScore()).isEqualTo(100 - 10); // 90, sommeil exclu
            assertThat(r.getTotalScore()).isLessThanOrEqualTo(90);
        }

        @Test
        @DisplayName("Humeur et sommeil absents → MOOD et SLEEP exclus, adjustedMaxScore = 80")
        void mood_and_sleep_absent_adjusted_max_80() {
            DailyLog log = createMinimalLog();
            log.setMoodLevel(null);
            log.setSleepHours(null);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(r.getMissingCategories()).containsExactlyInAnyOrder("MOOD", "SLEEP");
            assertThat(r.getAdjustedMaxScore()).isEqualTo(80);
            assertThat(findBreakdown(r, "MOOD").isExcluded()).isTrue();
            assertThat(findBreakdown(r, "SLEEP").isExcluded()).isTrue();
        }

        @Test
        @DisplayName("Risque calculé sur le pourcentage (totalScore / adjustedMaxScore)")
        void risk_based_on_percentage_when_sleep_missing() {
            DailyLog log = createLogWithHydration(1500);
            log.setMoodLevel("BONNE");
            log.setSleepHours(null);
            log.getActivityEntries().clear();
            log.getActivityEntries().add(createPhysicalActivity(30));
            log.getIncidentEntries().clear();
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(r.getAdjustedMaxScore()).isEqualTo(90);
            int total = r.getTotalScore();
            int pct = (total * 100) / 90;
            assertThat(r.getRiskLevel()).isEqualTo(pct >= 85 ? "Excellent" : pct >= 65 ? "Stable" : pct >= 45 ? "Risque moyen" : "Risque élevé");
        }
    }

    @Nested
    @DisplayName("Activité et humeur / sommeil")
    class ActivityAndMoodSleep {

        @Test
        @DisplayName("Activité ≥ 30 min → 20/20")
        void activity_ge_30_gives_20() {
            DailyLog log = createMinimalLog();
            log.getActivityEntries().clear();
            log.getActivityEntries().add(createPhysicalActivity(30));
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(findBreakdown(r, "ACTIVITY").getScore()).isEqualTo(20);
        }

        @Test
        @DisplayName("Humeur BONNE → 10/10, MAUVAISE → 2/10")
        void mood_scores() {
            DailyLog logGood = createMinimalLog();
            logGood.setMoodLevel("BONNE");
            logGood.setSleepHours(7.0);
            DailyLog logBad = createMinimalLog();
            logBad.setMoodLevel("MAUVAISE");
            logBad.setSleepHours(7.0);
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE))
                    .thenReturn(Optional.of(logGood))
                    .thenReturn(Optional.of(logBad));

            assertThat(healthScoreService.computeDailyScore(PATIENT_ID, DATE).getBreakdown().stream()
                    .filter(b -> "MOOD".equals(b.getCategory())).findFirst().orElseThrow().getScore()).isEqualTo(10);
            assertThat(healthScoreService.computeDailyScore(PATIENT_ID, DATE).getBreakdown().stream()
                    .filter(b -> "MOOD".equals(b.getCategory())).findFirst().orElseThrow().getScore()).isEqualTo(2);
        }

        @Test
        @DisplayName("Sommeil ≥ 7h → 10/10, < 5h → 2/10")
        void sleep_scores() {
            DailyLog log7 = createMinimalLog();
            log7.setMoodLevel("BONNE");
            log7.setSleepHours(7.5);
            DailyLog log4 = createMinimalLog();
            log4.setMoodLevel("BONNE");
            log4.setSleepHours(4.0);
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE))
                    .thenReturn(Optional.of(log7))
                    .thenReturn(Optional.of(log4));

            assertThat(healthScoreService.computeDailyScore(PATIENT_ID, DATE).getBreakdown().stream()
                    .filter(b -> "SLEEP".equals(b.getCategory())).findFirst().orElseThrow().getScore()).isEqualTo(10);
            assertThat(healthScoreService.computeDailyScore(PATIENT_ID, DATE).getBreakdown().stream()
                    .filter(b -> "SLEEP".equals(b.getCategory())).findFirst().orElseThrow().getScore()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Cas limites")
    class EdgeCases {

        @Test
        @DisplayName("Aucun log → humeur/sommeil exclus, adjustedMaxScore 80")
        void no_log_returns_adjusted_structure() {
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.empty());
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            assertThat(r.getAdjustedMaxScore()).isEqualTo(80);
            assertThat(r.getMissingCategories()).containsExactlyInAnyOrder("MOOD", "SLEEP");
            assertThat(r.getTotalScore()).isLessThanOrEqualTo(80);
            assertThat(r.getBreakdown()).hasSize(6);
        }

        @Test
        @DisplayName("Breakdown contient toutes les catégories")
        void breakdown_contains_all_categories() {
            DailyLog log = createMinimalLog();
            log.setMoodLevel("MOYENNE");
            log.setSleepHours(6.0);
            when(logRepo.findFirstByPatientKeycloakIdAndLogDate(PATIENT_ID, DATE)).thenReturn(Optional.of(log));
            when(medicationRepo.findByPrescriptionSessionMedicalFolderIdPatient(PATIENT_ID)).thenReturn(List.of());

            HealthScoreResponse r = healthScoreService.computeDailyScore(PATIENT_ID, DATE);

            List<String> categories = r.getBreakdown().stream().map(HealthScoreResponse.CategoryBreakdown::getCategory).toList();
            assertThat(categories).containsExactlyInAnyOrder("HYDRATATION", "MEDICATIONS", "ACTIVITY", "MOOD", "SLEEP", "INCIDENTS");
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
