package org.techhive.iotservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.iotservice.dto.SleepAnalysisResponse;
import org.techhive.iotservice.dto.SleepStageEntry;
import org.techhive.iotservice.dto.SleepSummary;
import org.techhive.iotservice.entity.HeartbeatReading;
import org.techhive.iotservice.repository.HeartbeatReadingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SleepAnalysisServiceTest {

    @Mock
    private HeartbeatReadingRepository heartbeatRepo;

    @InjectMocks
    private SleepAnalysisService sleepAnalysisService;

    private static final String PATIENT_ID = "patient-123";
    private static final LocalDate NIGHT_DATE = LocalDate.of(2026, 4, 10);

    @Test
    void analyze_noReadings_returnsEmptyTimeline() {
        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(List.of());

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(result.getDate()).isEqualTo(NIGHT_DATE);
        assertThat(result.getTimeline()).isEmpty();
        assertThat(result.getSummary().getTotalSleepMinutes()).isZero();
        assertThat(result.getSummary().getQualityLabel()).isEqualTo("No Data");
        assertThat(result.getInsights()).containsExactly("No heartbeat data available for this night.");
    }

    @Test
    void analyze_returnsCompleteResponse() {
        List<HeartbeatReading> readings = buildRealisticNightReadings();
        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(readings);

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        assertThat(result.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(result.getDate()).isEqualTo(NIGHT_DATE);
        assertThat(result.getTimeline()).isNotEmpty();
        assertThat(result.getSummary()).isNotNull();
        assertThat(result.getInsights()).isNotEmpty();
    }

    @Test
    void analyze_classifiesAwakeCorrectly() {
        // BPM > 76 should be classified as AWAKE
        List<HeartbeatReading> readings = List.of(
                reading(22, 0, 82),
                reading(22, 2, 85),
                reading(22, 4, 80)
        );
        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(readings);

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        assertThat(result.getTimeline()).allSatisfy(entry ->
                assertThat(entry.getStage()).isEqualTo("AWAKE"));
    }

    @Test
    void analyze_classifiesDeepSleepCorrectly() {
        // BPM < 58 with low variance should be DEEP in first half of night
        List<HeartbeatReading> readings = List.of(
                reading(23, 0, 52),
                reading(23, 2, 53),
                reading(23, 4, 51),
                reading(23, 6, 52),
                reading(23, 8, 53)
        );
        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(readings);

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        // All readings should be classified as DEEP (low BPM, low variance, early night)
        assertThat(result.getTimeline()).allSatisfy(entry ->
                assertThat(entry.getStage()).isEqualTo("DEEP"));
    }

    @Test
    void analyze_classifiesLightSleepCorrectly() {
        // Moderate BPM (60-76) with low variance in first half → LIGHT
        List<HeartbeatReading> readings = List.of(
                reading(22, 30, 65),
                reading(22, 32, 66),
                reading(22, 34, 64),
                reading(22, 36, 65),
                reading(22, 38, 66)
        );
        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(readings);

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        assertThat(result.getTimeline()).allSatisfy(entry ->
                assertThat(entry.getStage()).isEqualTo("LIGHT"));
    }

    @Test
    void analyze_computesSummaryCorrectly() {
        // Create readings that produce known stage distribution
        List<HeartbeatReading> readings = new ArrayList<>();
        // 5 DEEP readings (BPM ~52, low variance)
        for (int i = 0; i < 5; i++) {
            readings.add(reading(23, i * 2, 52));
        }
        // 5 LIGHT readings (BPM ~65, low variance)
        for (int i = 0; i < 5; i++) {
            readings.add(reading(23, 10 + i * 2, 65));
        }

        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(readings);

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        SleepSummary summary = result.getSummary();
        assertThat(summary.getTimeInBedMinutes()).isEqualTo(20); // 10 readings × 2 min
        assertThat(summary.getTotalSleepMinutes()).isGreaterThan(0);
        assertThat(summary.getSleepEfficiency()).isGreaterThan(0);
    }

    @Test
    void analyze_qualityScore_poor_forFragmentedSleep() {
        // Alternating AWAKE and LIGHT — fragmented, short sleep
        List<HeartbeatReading> readings = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            int bpm = (i % 2 == 0) ? 85 : 65; // alternate AWAKE/LIGHT
            readings.add(reading(22, 30 + i * 2, bpm));
        }

        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(readings);

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        // Short duration + many awakenings + no deep sleep → Poor quality
        assertThat(result.getSummary().getQualityScore()).isLessThan(50);
        assertThat(result.getSummary().getQualityLabel()).isEqualTo("Poor");
    }

    @Test
    void analyze_countsAwakenings() {
        // Use consecutive AWAKE readings so they survive smoothing
        List<HeartbeatReading> readings = List.of(
                reading(23, 0, 65),   // LIGHT
                reading(23, 2, 64),   // LIGHT
                reading(23, 4, 85),   // AWAKE (pair survives smoothing)
                reading(23, 6, 88),   // AWAKE
                reading(23, 8, 65),   // LIGHT
                reading(23, 10, 64),  // LIGHT
                reading(23, 12, 66)   // LIGHT
        );

        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(readings);

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        assertThat(result.getSummary().getAwakenings()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void analyze_generatesInsights() {
        List<HeartbeatReading> readings = buildRealisticNightReadings();
        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(readings);

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        // Should have at least 5 insight messages
        assertThat(result.getInsights()).hasSizeGreaterThanOrEqualTo(5);
        // Should contain deep sleep, REM, efficiency, total sleep insights
        String allInsights = String.join(" ", result.getInsights());
        assertThat(allInsights).containsIgnoringCase("deep sleep");
        assertThat(allInsights).containsIgnoringCase("REM");
        assertThat(allInsights).containsIgnoringCase("efficiency");
    }

    @Test
    void analyze_qualityLabelMapping() {
        // Full night of ideal deep sleep → should get high quality score
        List<HeartbeatReading> readings = new ArrayList<>();
        // 240 readings at 2-min intervals = 8 hours — mix of deep + light + REM
        LocalDateTime start = NIGHT_DATE.atTime(22, 0);
        for (int i = 0; i < 240; i++) {
            int bpm;
            if (i < 60) bpm = 52;       // Deep (first 2h)
            else if (i < 120) bpm = 65;  // Light (2h)
            else if (i < 180) bpm = 52;  // Deep (2h)
            else bpm = 65;               // Light (2h)
            readings.add(HeartbeatReading.builder()
                    .id((long) i)
                    .patientId(PATIENT_ID)
                    .bpm(bpm)
                    .timestamp(start.plusMinutes(i * 2L))
                    .build());
        }

        when(heartbeatRepo.findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
                eq(PATIENT_ID), any(), any()))
                .thenReturn(readings);

        SleepAnalysisResponse result = sleepAnalysisService.analyze(PATIENT_ID, NIGHT_DATE);

        // With 8h of sleep and deep sleep, should score well
        assertThat(result.getSummary().getQualityLabel())
                .isIn("Good", "Excellent");
        assertThat(result.getSummary().getQualityScore()).isGreaterThanOrEqualTo(70);
    }

    // ─── Helpers ───────────────────────────────────────────

    private HeartbeatReading reading(int hour, int minute, int bpm) {
        LocalDateTime ts = hour >= 12
                ? NIGHT_DATE.atTime(hour, minute)
                : NIGHT_DATE.plusDays(1).atTime(hour, minute);
        return HeartbeatReading.builder()
                .id((long) (hour * 100 + minute))
                .patientId(PATIENT_ID)
                .bpm(bpm)
                .timestamp(ts)
                .build();
    }

    /**
     * Build realistic night readings: 22:00 → 06:00, 2-min intervals (240 readings).
     */
    private List<HeartbeatReading> buildRealisticNightReadings() {
        List<HeartbeatReading> readings = new ArrayList<>();
        LocalDateTime start = NIGHT_DATE.atTime(22, 0);

        for (int i = 0; i < 240; i++) {
            int bpm;
            double hoursIn = i * 2.0 / 60.0;

            if (hoursIn < 0.5) {
                bpm = 78 - (int) (hoursIn * 20);  // Falling asleep
            } else if (hoursIn < 2.0) {
                bpm = 55 + (i % 5);               // Deep sleep
            } else if (hoursIn < 4.0) {
                bpm = 62 + (i % 6);               // Light sleep
            } else if (hoursIn < 6.0) {
                bpm = 58 + (i % 8);               // Mix
            } else {
                bpm = 68 + (int) ((hoursIn - 6) * 5); // Waking up
            }

            readings.add(HeartbeatReading.builder()
                    .id((long) i)
                    .patientId(PATIENT_ID)
                    .bpm(bpm)
                    .timestamp(start.plusMinutes(i * 2L))
                    .build());
        }
        return readings;
    }
}
