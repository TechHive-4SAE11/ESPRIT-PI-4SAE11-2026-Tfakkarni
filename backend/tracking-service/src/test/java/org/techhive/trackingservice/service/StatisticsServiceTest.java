package org.techhive.trackingservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.trackingservice.dto.*;
import org.techhive.trackingservice.entity.*;
import org.techhive.trackingservice.repository.DailyLogRepository;
import org.techhive.trackingservice.repository.MedicationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    private static final String PATIENT_ID = "patient-stat-1";
    private static final LocalDate START = LocalDate.of(2026, 5, 1);
    private static final LocalDate END = LocalDate.of(2026, 5, 3);

    @Mock
    private DailyLogRepository logRepo;

    @Mock
    private MedicationRepository medicationRepo;

    @Mock
    private HealthScoreService healthScoreService;

    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(logRepo, medicationRepo, healthScoreService);
    }

    @Test
    void getScoreTrend_formatsDatesAndComputesPercentagesFromAdjustedMaxScore() {
        when(healthScoreService.computeDailyScore(PATIENT_ID, START))
                .thenReturn(healthScore(45, 50));
        when(healthScoreService.computeDailyScore(PATIENT_ID, START.plusDays(1)))
                .thenReturn(healthScore(0, 0));
        when(healthScoreService.computeDailyScore(PATIENT_ID, END))
                .thenReturn(healthScore(70, 100));

        ScoreTrendResponse response = statisticsService.getScoreTrend(PATIENT_ID, START, END);

        assertThat(response.getDates()).containsExactly("01 mai", "02 mai", "03 mai");
        assertThat(response.getScores()).containsExactly(90, 0, 70);
    }

    @Test
    void getIncidentTypes_countsSortsAndLabelsKnownAndUnknownTypes() {
        DailyLog first = dailyLog(START);
        first.getIncidentEntries().add(incident("CHUTE"));
        first.getIncidentEntries().add(incident("CHUTE"));
        first.getIncidentEntries().add(incident(null));
        DailyLog second = dailyLog(START.plusDays(1));
        second.getIncidentEntries().add(incident("CUSTOM"));
        when(logRepo.findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(PATIENT_ID, START, END))
                .thenReturn(List.of(first, second));

        IncidentStatsResponse response = statisticsService.getIncidentTypes(PATIENT_ID, START, END);

        assertThat(response.getLabels()).containsExactly("Chute", "Autre", "CUSTOM");
        assertThat(response.getValues()).containsExactly(2, 1, 1);
    }

    @Test
    void getMedicationCompliance_countsTakenMissedAndBuildsDailyHistory() {
        Medication doliprane = medication("Doliprane", START, END);
        DailyLog first = dailyLog(START);
        first.getMedicationIntakes().add(intake(doliprane, "PRIS"));
        first.getMedicationIntakes().add(intake(doliprane, "OUBLIE"));
        DailyLog second = dailyLog(START.plusDays(1));
        second.getMedicationIntakes().add(intake(doliprane, "PRIS"));
        when(logRepo.findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(PATIENT_ID, START, END))
                .thenReturn(List.of(first, second));

        MedicationComplianceResponse response = statisticsService.getMedicationCompliance(PATIENT_ID, START, END);

        assertThat(response.getStartDate()).isEqualTo("2026-05-01");
        assertThat(response.getEndDate()).isEqualTo("2026-05-03");
        assertThat(response.getTaken()).isEqualTo(2);
        assertThat(response.getMissed()).isEqualTo(1);
        assertThat(response.getHistory()).hasSize(3);
        assertThat(response.getHistory().get(0).getDate()).isEqualTo("2026-05-01");
        assertThat(response.getHistory().get(0).getComplianceRate()).isEqualTo(50.0);
        assertThat(response.getHistory().get(1).getComplianceRate()).isEqualTo(100.0);
        assertThat(response.getHistory().get(2).getComplianceRate()).isZero();
    }

    @Test
    void getMedicationCompliance_usesDemoFallbackForSeedPatientWithNoSchedule() {
        when(logRepo.findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc("patient", START, START))
                .thenReturn(List.of());

        MedicationComplianceResponse response = statisticsService.getMedicationCompliance("patient", START, START);

        assertThat(response.getTaken()).isZero();
        assertThat(response.getMissed()).isZero();
        assertThat(response.getHistory()).hasSize(1);
        assertThat(response.getHistory().get(0).getComplianceRate()).isBetween(92.0, 98.0);
    }

    @Test
    void getMedicationComplianceByDrug_aggregatesTakenAndMissedByMedicationName() {
        Medication doliprane = medication("Doliprane", START, END);
        Medication amoxicilline = medication("Amoxicilline", null, null);
        DailyLog log = dailyLog(START);
        log.getMedicationIntakes().add(intake(doliprane, "PRIS"));
        log.getMedicationIntakes().add(intake(doliprane, "REFUSE"));
        log.getMedicationIntakes().add(intake(amoxicilline, "PRIS"));
        when(logRepo.findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(PATIENT_ID, START, END))
                .thenReturn(List.of(log));

        List<MedicationComplianceResponse> response = statisticsService.getMedicationComplianceByDrug(PATIENT_ID, START, END);

        assertThat(response).hasSize(2);
        MedicationComplianceResponse dolipraneStats = response.stream()
                .filter(r -> "Doliprane".equals(r.getMedicationName()))
                .findFirst()
                .orElseThrow();
        assertThat(dolipraneStats.getStartDate()).isEqualTo("2026-05-01");
        assertThat(dolipraneStats.getEndDate()).isEqualTo("2026-05-03");
        assertThat(dolipraneStats.getTaken()).isEqualTo(1);
        assertThat(dolipraneStats.getMissed()).isEqualTo(1);
        MedicationComplianceResponse amoxicillineStats = response.stream()
                .filter(r -> "Amoxicilline".equals(r.getMedicationName()))
                .findFirst()
                .orElseThrow();
        assertThat(amoxicillineStats.getStartDate()).isEqualTo("2026-05-01");
        assertThat(amoxicillineStats.getEndDate()).isEqualTo("2026-05-03");
        assertThat(amoxicillineStats.getTaken()).isEqualTo(1);
        assertThat(amoxicillineStats.getMissed()).isZero();
    }

    @Test
    void getHydrationTrend_sumsHydrationByDayAndKeepsMissingDaysAtZero() {
        DailyLog first = dailyLog(START);
        first.getNutritionEntries().add(nutrition(500));
        first.getNutritionEntries().add(nutrition(null));
        DailyLog third = dailyLog(END);
        third.getNutritionEntries().add(nutrition(750));
        when(logRepo.findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(PATIENT_ID, START, END))
                .thenReturn(List.of(first, third));

        HydrationTrendResponse response = statisticsService.getHydrationTrend(PATIENT_ID, START, END);

        assertThat(response.getDates()).containsExactly("01 mai", "02 mai", "03 mai");
        assertThat(response.getValues()).containsExactly(500, 0, 750);
    }

    @Test
    void getActivityTrend_sumsOnlyPhysicalActivities() {
        DailyLog first = dailyLog(START);
        first.getActivityEntries().add(activity("PHYSIQUE", 20));
        first.getActivityEntries().add(activity("COGNITIVE", 30));
        DailyLog second = dailyLog(START.plusDays(1));
        second.getActivityEntries().add(activity("PHYSIQUE", null));
        when(logRepo.findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(PATIENT_ID, START, END))
                .thenReturn(List.of(first, second));

        ActivityTrendResponse response = statisticsService.getActivityTrend(PATIENT_ID, START, END);

        assertThat(response.getDates()).containsExactly("01 mai", "02 mai", "03 mai");
        assertThat(response.getValues()).containsExactly(20, 0, 0);
    }

    @Test
    void getStreak_countsQualifiedDaysAndBuildsCalendarFromFirstLogDate() {
        LocalDate today = LocalDate.now();
        DailyLog firstLog = dailyLog(today.minusDays(4));
        when(logRepo.findFirstByPatientKeycloakIdOrderByLogDateAsc(PATIENT_ID)).thenReturn(Optional.of(firstLog));
        when(healthScoreService.computeDailyScore(eq(PATIENT_ID), eq(today.minusDays(4)))).thenReturn(healthScore(90, 100));
        when(healthScoreService.computeDailyScore(eq(PATIENT_ID), eq(today.minusDays(3)))).thenReturn(healthScore(60, 100));
        when(healthScoreService.computeDailyScore(eq(PATIENT_ID), eq(today.minusDays(2)))).thenReturn(healthScore(95, 100));
        when(healthScoreService.computeDailyScore(eq(PATIENT_ID), eq(today.minusDays(1)))).thenReturn(healthScore(40, 100));
        when(healthScoreService.computeDailyScore(eq(PATIENT_ID), eq(today))).thenReturn(healthScore(100, 100));

        StreakResponse response = statisticsService.getStreak(PATIENT_ID);

        assertThat(response.getCurrentStreak()).isEqualTo(2);
        assertThat(response.getLivesRemaining()).isZero();
        assertThat(response.isPremiumUnlocked()).isFalse();
        assertThat(response.getLast14Days()).hasSize(14);
        assertThat(response.getLast14Days().get(0).isToday()).isTrue();
        assertThat(response.getLast14Days().get(0).isPassed()).isTrue();
        assertThat(response.getLast14Days().get(0).isActive()).isTrue();
        assertThat(response.getLast14Days().get(5).isActive()).isFalse();
    }

    @Test
    void getStreak_withoutLogsStartsTodayAndKeepsLives() {
        LocalDate today = LocalDate.now();
        when(logRepo.findFirstByPatientKeycloakIdOrderByLogDateAsc(PATIENT_ID)).thenReturn(Optional.empty());
        when(healthScoreService.computeDailyScore(eq(PATIENT_ID), eq(today))).thenReturn(healthScore(0, 0));

        StreakResponse response = statisticsService.getStreak(PATIENT_ID);

        assertThat(response.getCurrentStreak()).isZero();
        assertThat(response.getLivesRemaining()).isEqualTo(2);
        assertThat(response.getLast14Days().get(0).isActive()).isTrue();
        assertThat(response.getLast14Days().get(1).isActive()).isFalse();
    }

    private static HealthScoreResponse healthScore(int totalScore, int adjustedMaxScore) {
        return HealthScoreResponse.builder()
                .totalScore(totalScore)
                .adjustedMaxScore(adjustedMaxScore)
                .build();
    }

    private static DailyLog dailyLog(LocalDate date) {
        DailyLog log = new DailyLog();
        log.setPatientKeycloakId(PATIENT_ID);
        log.setLogDate(date);
        return log;
    }

    private static IncidentEntry incident(String type) {
        IncidentEntry incident = new IncidentEntry();
        incident.setIncidentType(type);
        return incident;
    }

    private static NutritionEntry nutrition(Integer hydrationMl) {
        NutritionEntry nutrition = new NutritionEntry();
        nutrition.setHydrationMl(hydrationMl);
        return nutrition;
    }

    private static ActivityEntry activity(String type, Integer durationMinutes) {
        ActivityEntry activity = new ActivityEntry();
        activity.setActivityType(type);
        activity.setDurationMinutes(durationMinutes);
        return activity;
    }

    private static Medication medication(String name, LocalDate startDate, LocalDate endDate) {
        Medication medication = new Medication();
        medication.setMedicationName(name);
        medication.setStartDate(startDate);
        medication.setEndDate(endDate);
        return medication;
    }

    private static MedicationIntakeLog intake(Medication medication, String status) {
        MedicationIntakeLog intake = new MedicationIntakeLog();
        intake.setMedication(medication);
        intake.setStatus(status);
        return intake;
    }
}
