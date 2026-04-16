package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.analyticsservice.entity.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientScoreResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String patientKeycloakId;
    private Double cognitiveScore;
    private Double dailyFunctioningScore;
    private Double medicalStabilityScore;
    private Double iotRiskScore;
    private Double engagementScore;
    private Double overallScore;
    private AlzheimerStage stage;
    private ScoreTrend scoreTrend;
    private LocalDateTime computedAt;
    private List<CognitiveDomainDTO> cognitiveDomains;
}
