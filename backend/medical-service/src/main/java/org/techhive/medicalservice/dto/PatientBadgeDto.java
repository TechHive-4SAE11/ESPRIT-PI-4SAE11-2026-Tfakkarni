package org.techhive.medicalservice.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientBadgeDto {
    private Long id;
    private String patientId;
    private String badgeCode;
    private String badgeTitle;
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime awardedAt;

    private String sourceGameType;
    private Long sourceAttemptId;
}
