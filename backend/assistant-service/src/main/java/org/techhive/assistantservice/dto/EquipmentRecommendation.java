package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentRecommendation {
    private Long equipmentId;
    private String equipmentName;
    private String category;
    private String justification;
    private Double relevanceScore;  // 0.0 to 1.0
    private String usageInstructions;
}
