package org.techhive.iotservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepHistoryResponse {

    private String patientId;
    private int days;
    private List<DailySleepEntry> entries;
    private WeeklySummary weeklySummary;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailySleepEntry {
        private LocalDate date;
        private SleepSummary summary;
        private List<String> insights;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklySummary {
        private double avgQualityScore;
        private String avgQualityLabel;
        private int avgTotalSleepMinutes;
        private double avgDeepSleepPercent;
        private double avgEfficiency;
        private int totalAwakenings;
        private int nightsWithData;
        private LocalDate bestNight;
        private int bestNightScore;
        private LocalDate worstNight;
        private int worstNightScore;
        private String trend; // IMPROVING, STABLE, DECLINING
        private List<String> weeklyInsights;
    }
}
