package org.techhive.medicalservice.dto.coaching;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.medicalservice.entity.coaching.CoachingMood;
import org.techhive.medicalservice.entity.coaching.ProgressRecordedByRole;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingProgressRequest {

    private LocalDate dateRecorded;

    @Min(0)
    @Max(100)
    private Integer completionPercentage;

    private CoachingMood mood;

    @Min(1)
    @Max(10)
    private Integer energyLevel;

    private String helperNotes;
    private String patientFeedback;

    @NotNull
    private ProgressRecordedByRole recordedByRole;
}
