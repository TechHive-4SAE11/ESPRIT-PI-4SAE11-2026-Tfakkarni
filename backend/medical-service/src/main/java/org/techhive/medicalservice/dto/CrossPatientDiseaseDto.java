package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossPatientDiseaseDto {

    private Long diagnosticsId;
    private Long medicalFolderId;
    private String patientId;
    /** Resolved via user-service (Keycloak id). */
    private String patientDisplayName;
    private String doctorId;
    /** Resolved via user-service (Keycloak id). */
    private String doctorDisplayName;
    private String diseaseName;
    private String stage;
    private LocalDateTime diagnosisDate;
}
