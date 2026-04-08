package org.techhive.medicalservice.dto.coaching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.medicalservice.entity.coaching.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingGoalResponse {
    private Long id;
    private Long medicalFolderId;
    private Long diagnosticId;
    private CoachingGoalType goalType;
    private String goalTitle;
    private String actionSteps;
    private String tips;
    private Integer targetDays;
    private CoachingGoalStatus status;
    private CoachingPriority priority;
    private boolean outdoorActivity;
    private Double latitude;
    private Double longitude;
    private String createdByDoctorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastStaleNotificationAt;
}
