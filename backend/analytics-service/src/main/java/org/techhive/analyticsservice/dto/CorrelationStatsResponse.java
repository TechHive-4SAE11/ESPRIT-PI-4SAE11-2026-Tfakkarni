package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrelationStatsResponse {
    private String patientKeycloakId;
    
    // Correlation points: Date -> { gameScore: X, medicationCompliance: Y, incidentCount: Z }
    private List<DailyCorrelationPoint> correlationTimeline;
    
    // Summary insights
    private String keyInsight; // e.g. "Game performance drops on missed medication days"
    private double adherenceCorrelation; // -1 to 1 correlation coefficient between adherence and game scores
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyCorrelationPoint {
        private String date;
        private Double avgGameScore;
        private Double medicationAdherence; // 0.0 to 1.0
        private Integer incidentCount;
    }
}
