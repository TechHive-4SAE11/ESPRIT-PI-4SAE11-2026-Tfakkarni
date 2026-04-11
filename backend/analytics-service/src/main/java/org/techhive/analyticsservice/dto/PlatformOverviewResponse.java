package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.analyticsservice.entity.AlzheimerStage;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformOverviewResponse {
    private int totalPatients;
    private int totalDoctors;
    private Map<AlzheimerStage, Integer> stageDistribution;
    private Double platformAvgScore;
    private Map<String, Integer> cognitiveDomainWeakness;
    private int totalGameAttempts;
    private int totalIncidents;
    private int redFlagDoctorCount;
}
