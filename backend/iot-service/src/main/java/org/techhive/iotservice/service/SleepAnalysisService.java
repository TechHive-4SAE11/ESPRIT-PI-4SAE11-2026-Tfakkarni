package org.techhive.iotservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.techhive.iotservice.dto.*;
import org.techhive.iotservice.entity.HeartbeatReading;
import org.techhive.iotservice.repository.HeartbeatReadingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SleepAnalysisService {

    private final HeartbeatReadingRepository heartbeatRepo;

    private static final int INTERVAL_MINUTES = 2;

    /**
     * Perform full sleep analysis for a patient on a given night.
     */
    public SleepAnalysisResponse analyze(String patientId, LocalDate date) {
        LocalDateTime start = date.atTime(LocalTime.of(20, 0));
        LocalDateTime end = date.plusDays(1).atTime(LocalTime.of(12, 0));

        List<HeartbeatReading> readings = heartbeatRepo
                .findByPatientIdAndTimestampBetweenOrderByTimestampAsc(patientId, start, end);

        if (readings.isEmpty()) {
            return SleepAnalysisResponse.builder()
                    .patientId(patientId)
                    .date(date)
                    .timeline(List.of())
                    .summary(SleepSummary.builder()
                            .totalSleepMinutes(0)
                            .timeInBedMinutes(0)
                            .qualityScore(0)
                            .qualityLabel("No Data")
                            .build())
                    .insights(List.of("No heartbeat data available for this night."))
                    .build();
        }

        // Classify each reading into a sleep stage
        List<SleepStageEntry> timeline = classifyStages(readings);

        // Compute summary
        SleepSummary summary = computeSummary(timeline, readings);

        // Generate insights
        List<String> insights = generateInsights(summary);

        return SleepAnalysisResponse.builder()
                .patientId(patientId)
                .date(date)
                .timeline(timeline)
                .summary(summary)
                .insights(insights)
                .build();
    }

    /**
     * Classify each heartbeat reading into a sleep stage using BPM thresholds,
     * local variability (sliding window), and time-of-night context.
     */
    private List<SleepStageEntry> classifyStages(List<HeartbeatReading> readings) {
        List<SleepStageEntry> timeline = new ArrayList<>();
        int size = readings.size();

        for (int i = 0; i < size; i++) {
            HeartbeatReading current = readings.get(i);
            int bpm = current.getBpm();

            // Compute local variance using a window of up to 3 readings
            double variance = computeLocalVariance(readings, i, 3);

            // Time context: hours since 22:00 (sleep start)
            double hoursIntoNight = getHoursIntoNight(current.getTimestamp());

            String stage = classifySingleReading(bpm, variance, hoursIntoNight);

            timeline.add(SleepStageEntry.builder()
                    .timestamp(current.getTimestamp())
                    .bpm(bpm)
                    .stage(stage)
                    .build());
        }

        // Smooth: remove isolated single-reading stage changes (noise reduction)
        smoothTimeline(timeline);

        return timeline;
    }

    private String classifySingleReading(int bpm, double variance, double hoursIntoNight) {
        // Awake: high BPM
        if (bpm > 76) {
            return "AWAKE";
        }

        // Deep sleep: very low BPM, low variance, more common in first half
        if (bpm < 58 && variance < 15.0) {
            return "DEEP";
        }

        // REM: moderate BPM with high variability, more common in second half
        if (bpm >= 60 && bpm <= 78 && variance > 20.0) {
            // REM is more likely in the second half of the night
            if (hoursIntoNight > 3.0) {
                return "REM";
            }
            // Early night high variance more likely light sleep transition
            return bpm > 68 ? "AWAKE" : "LIGHT";
        }

        // REM boost in second half: slightly wider range
        if (hoursIntoNight > 4.0 && bpm >= 58 && bpm <= 75 && variance > 12.0) {
            return "REM";
        }

        // Deep sleep boost in first half
        if (hoursIntoNight < 4.0 && bpm < 62 && variance < 20.0) {
            return "DEEP";
        }

        // Default: light sleep
        return "LIGHT";
    }

    private double computeLocalVariance(List<HeartbeatReading> readings, int index, int windowSize) {
        int start = Math.max(0, index - windowSize / 2);
        int end = Math.min(readings.size(), index + windowSize / 2 + 1);

        if (end - start < 2) return 0.0;

        double sum = 0;
        int count = 0;
        for (int i = start; i < end; i++) {
            sum += readings.get(i).getBpm();
            count++;
        }
        double mean = sum / count;

        double varianceSum = 0;
        for (int i = start; i < end; i++) {
            double diff = readings.get(i).getBpm() - mean;
            varianceSum += diff * diff;
        }
        return varianceSum / count;
    }

    private double getHoursIntoNight(LocalDateTime timestamp) {
        // Reference: 22:00 of the night
        LocalDateTime sleepStart = timestamp.toLocalDate().atTime(22, 0);
        if (timestamp.getHour() < 12) {
            // After midnight - reference is previous day's 22:00
            sleepStart = timestamp.toLocalDate().minusDays(1).atTime(22, 0);
        }
        return Duration.between(sleepStart, timestamp).toMinutes() / 60.0;
    }

    /**
     * Smooth out isolated stage flips (single reading different from neighbors).
     */
    private void smoothTimeline(List<SleepStageEntry> timeline) {
        for (int i = 1; i < timeline.size() - 1; i++) {
            String prev = timeline.get(i - 1).getStage();
            String curr = timeline.get(i).getStage();
            String next = timeline.get(i + 1).getStage();
            if (prev.equals(next) && !curr.equals(prev)) {
                timeline.get(i).setStage(prev);
            }
        }
    }

    private SleepSummary computeSummary(List<SleepStageEntry> timeline, List<HeartbeatReading> readings) {
        int deepMinutes = 0, lightMinutes = 0, remMinutes = 0, awakeMinutes = 0;
        int awakenings = 0;
        boolean wasAsleep = false;

        for (int i = 0; i < timeline.size(); i++) {
            String stage = timeline.get(i).getStage();
            switch (stage) {
                case "DEEP" -> deepMinutes += INTERVAL_MINUTES;
                case "LIGHT" -> lightMinutes += INTERVAL_MINUTES;
                case "REM" -> remMinutes += INTERVAL_MINUTES;
                case "AWAKE" -> awakeMinutes += INTERVAL_MINUTES;
            }

            boolean isAsleep = !"AWAKE".equals(stage);
            if (wasAsleep && !isAsleep) {
                awakenings++;
            }
            wasAsleep = isAsleep;
        }

        int totalSleep = deepMinutes + lightMinutes + remMinutes;
        int timeInBed = timeline.size() * INTERVAL_MINUTES;
        double efficiency = timeInBed > 0 ? (totalSleep * 100.0 / timeInBed) : 0;

        // Quality score (0-100) based on deep sleep %, awakenings, total duration, efficiency
        int qualityScore = computeQualityScore(deepMinutes, totalSleep, awakenings, efficiency);
        String qualityLabel = qualityScore >= 85 ? "Excellent"
                : qualityScore >= 70 ? "Good"
                : qualityScore >= 50 ? "Fair"
                : "Poor";

        double total = Math.max(1, deepMinutes + lightMinutes + remMinutes + awakeMinutes);

        return SleepSummary.builder()
                .totalSleepMinutes(totalSleep)
                .timeInBedMinutes(timeInBed)
                .deepSleepMinutes(deepMinutes)
                .lightSleepMinutes(lightMinutes)
                .remSleepMinutes(remMinutes)
                .awakeMinutes(awakeMinutes)
                .deepSleepPercent(Math.round(deepMinutes * 1000.0 / total) / 10.0)
                .lightSleepPercent(Math.round(lightMinutes * 1000.0 / total) / 10.0)
                .remSleepPercent(Math.round(remMinutes * 1000.0 / total) / 10.0)
                .awakePercent(Math.round(awakeMinutes * 1000.0 / total) / 10.0)
                .sleepEfficiency(Math.round(efficiency * 10.0) / 10.0)
                .qualityScore(qualityScore)
                .awakenings(awakenings)
                .qualityLabel(qualityLabel)
                .build();
    }

    private int computeQualityScore(int deepMinutes, int totalSleep, int awakenings, double efficiency) {
        int score = 0;

        // Deep sleep contribution (ideal: 60-120 min, i.e. 13-23% of 7.5h)
        if (deepMinutes >= 60 && deepMinutes <= 120) {
            score += 30;
        } else if (deepMinutes >= 40) {
            score += 20;
        } else if (deepMinutes >= 20) {
            score += 10;
        }

        // Total sleep contribution (ideal: 420-540 min = 7-9h)
        if (totalSleep >= 420 && totalSleep <= 540) {
            score += 30;
        } else if (totalSleep >= 360) {
            score += 20;
        } else if (totalSleep >= 300) {
            score += 15;
        } else if (totalSleep >= 240) {
            score += 10;
        }

        // Awakenings (fewer is better)
        if (awakenings == 0) {
            score += 20;
        } else if (awakenings <= 2) {
            score += 15;
        } else if (awakenings <= 4) {
            score += 10;
        } else {
            score += 5;
        }

        // Sleep efficiency
        if (efficiency >= 90) {
            score += 20;
        } else if (efficiency >= 80) {
            score += 15;
        } else if (efficiency >= 70) {
            score += 10;
        } else {
            score += 5;
        }

        return Math.min(100, score);
    }

    private List<String> generateInsights(SleepSummary summary) {
        List<String> insights = new ArrayList<>();

        // Deep sleep insight
        int deepH = summary.getDeepSleepMinutes() / 60;
        int deepM = summary.getDeepSleepMinutes() % 60;
        insights.add(String.format("You spent %dh %dm in deep sleep (restorative phase) — %.1f%% of total",
                deepH, deepM, summary.getDeepSleepPercent()));

        // REM insight
        int remH = summary.getRemSleepMinutes() / 60;
        int remM = summary.getRemSleepMinutes() % 60;
        insights.add(String.format("REM sleep: %dh %dm (%.1f%%) — important for memory consolidation",
                remH, remM, summary.getRemSleepPercent()));

        // Awakenings
        if (summary.getAwakenings() == 0) {
            insights.add("No awakenings detected — excellent sleep continuity!");
        } else {
            insights.add(String.format("%d awakening%s during the night",
                    summary.getAwakenings(), summary.getAwakenings() > 1 ? "s" : ""));
        }

        // Efficiency
        String effLabel = summary.getSleepEfficiency() >= 90 ? "Excellent"
                : summary.getSleepEfficiency() >= 80 ? "Good"
                : summary.getSleepEfficiency() >= 70 ? "Fair"
                : "Needs improvement";
        insights.add(String.format("Sleep efficiency: %.1f%% (%s)",
                summary.getSleepEfficiency(), effLabel));

        // Total sleep
        int totalH = summary.getTotalSleepMinutes() / 60;
        int totalM = summary.getTotalSleepMinutes() % 60;
        if (summary.getTotalSleepMinutes() < 360) {
            insights.add(String.format("Total sleep: %dh %dm — below recommended 7-9 hours", totalH, totalM));
        } else if (summary.getTotalSleepMinutes() > 540) {
            insights.add(String.format("Total sleep: %dh %dm — above average, monitor for oversleeping", totalH, totalM));
        } else {
            insights.add(String.format("Total sleep: %dh %dm — within healthy range", totalH, totalM));
        }

        return insights;
    }
}
