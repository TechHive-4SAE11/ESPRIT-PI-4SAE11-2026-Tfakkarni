package org.techhive.mlservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomPilotResponse {
    private List<Prediction> predictions;
    private boolean isCriticalAlert;
    private String alertMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Prediction {
        private String condition;
        private double probability;
        private String riskLevel; // LOW, MODERATE, HIGH
    }
}
