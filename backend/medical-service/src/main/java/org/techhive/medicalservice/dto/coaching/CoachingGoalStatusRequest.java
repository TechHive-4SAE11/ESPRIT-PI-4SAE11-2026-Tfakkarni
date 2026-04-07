package org.techhive.medicalservice.dto.coaching;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.medicalservice.entity.coaching.CoachingGoalStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingGoalStatusRequest {

    @NotNull
    private CoachingGoalStatus status;
}
