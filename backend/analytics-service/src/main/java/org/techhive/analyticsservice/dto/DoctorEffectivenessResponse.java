package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorEffectivenessResponse {
    private String doctorKeycloakId;
    private String doctorName;
    private int patientCount;
    private Double stabilizationRate;
    private Double declineRate;
    private Double avgComplianceImprovement;
    private Double sessionFrequency;
    private Double coachingCompletionRate;
    private Double appointmentShowRate;
    private List<String> riskFlags;
    private LocalDateTime computedAt;
}
