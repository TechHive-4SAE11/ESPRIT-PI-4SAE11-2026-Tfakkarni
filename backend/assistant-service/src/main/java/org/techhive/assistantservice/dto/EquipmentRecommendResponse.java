package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentRecommendResponse {
    private Long patientId;
    private String condition;
    private String severity;
    private List<EquipmentRecommendation> recommendations;
    private String generalAdvice;
}
