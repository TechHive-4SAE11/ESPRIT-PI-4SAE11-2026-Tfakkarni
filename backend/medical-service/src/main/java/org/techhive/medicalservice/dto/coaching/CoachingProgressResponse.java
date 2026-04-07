package org.techhive.medicalservice.dto.coaching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.medicalservice.entity.coaching.CoachingMood;
import org.techhive.medicalservice.entity.coaching.ProgressRecordedByRole;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingProgressResponse {
    private Long id;
    private Long coachingGoalId;
    private LocalDate dateRecorded;
    private Integer completionPercentage;
    private CoachingMood mood;
    private Integer energyLevel;
    private String helperNotes;
    private String patientFeedback;
    private ProgressRecordedByRole recordedByRole;
    private String recordedByUserId;
    private String weatherSummary;
    private LocalDateTime weatherFetchedAt;
    private LocalDateTime createdAt;
}
