package org.techhive.iotservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SleepSummary {
    private int totalSleepMinutes;
    private int timeInBedMinutes;
    private int deepSleepMinutes;
    private int lightSleepMinutes;
    private int remSleepMinutes;
    private int awakeMinutes;
    private double deepSleepPercent;
    private double lightSleepPercent;
    private double remSleepPercent;
    private double awakePercent;
    private double sleepEfficiency;
    private int qualityScore;
    private int awakenings;
    private String qualityLabel; // Poor, Fair, Good, Excellent
}
