package org.techhive.mlservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleImpactDTO {
    private Long moduleId;
    private String moduleTitle;
    private String moduleCategory;
    private LocalDateTime completedDate;
    private Integer stressBefore;
    private Integer stressAfter;
    private Integer stressImprovement;
    private Integer observanceBefore;
    private Integer observanceAfter;
    private Integer observanceImprovement;
    private String impactMessage;
}
