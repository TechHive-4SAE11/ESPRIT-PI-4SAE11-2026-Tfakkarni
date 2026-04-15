package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.techhive.medicalservice.entity.AttendanceRiskLevel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlaggedPatientDto {
    private Long medicalFolderId;
    private String patientId;
    private String patientDisplayName;
    private int consecutiveNoShows;
    private AttendanceRiskLevel attendanceRiskLevel;
    private boolean bookingRestricted;
    private boolean manualReviewRequired;
    private String restrictionReason;
}
