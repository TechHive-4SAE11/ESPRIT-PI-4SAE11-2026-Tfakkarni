package org.techhive.medicalservice.dto.coaching;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.medicalservice.entity.coaching.CoachingGoalType;
import org.techhive.medicalservice.entity.coaching.CoachingPriority;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingGoalRequest {

    private Long diagnosticId;

    @NotNull
    private CoachingGoalType goalType;

    @NotBlank
    @Size(max = 500)
    private String goalTitle;

    private String actionSteps;
    private String tips;

    private Integer targetDays;
    private CoachingPriority priority;

    @Builder.Default
    private boolean outdoorActivity = false;

    private Double latitude;
    private Double longitude;
}
