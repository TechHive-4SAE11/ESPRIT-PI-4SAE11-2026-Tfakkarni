package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.analyticsservice.entity.ScoreTrend;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CognitiveDomainDTO {
    private String domainName;
    private int correctCount;
    private int incorrectCount;
    private Double accuracyPct;
    private ScoreTrend trend;
}
