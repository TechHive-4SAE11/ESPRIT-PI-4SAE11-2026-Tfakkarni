package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionDTO {
    private Integer riskScore;
    private String riskLevel;
    private Map<String, Object> factors;
    private String recommendation;
}
