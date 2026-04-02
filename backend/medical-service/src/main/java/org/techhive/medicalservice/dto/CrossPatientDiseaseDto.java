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
    private String doctorId;
    private String diseaseName;
    private String stage;
    private LocalDateTime diagnosisDate;
}
