package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataPointPerformanceDTO {
    private Long id;
    private String patientKeycloakId;
    private String dataType;
    private Long dataPointId;
    private int correctCount;
    private int incorrectCount;
    private boolean lastCorrect;
}
