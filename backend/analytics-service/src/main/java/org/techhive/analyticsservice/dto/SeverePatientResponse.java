package org.techhive.analyticsservice.dto;

import lombok.*;
import org.techhive.analyticsservice.entity.AlzheimerStage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeverePatientResponse {
    private String patientKeycloakId;
    private String patientName;
    private AlzheimerStage stage;
    private double overallScore;
    private double cognitiveScore;
    private String currentDoctorKeycloakId;
    private String currentDoctorName;
    private String recommendedDoctorKeycloakId;
    private String recommendedDoctorName;
    private double recommendedDoctorMatchScore;
}
