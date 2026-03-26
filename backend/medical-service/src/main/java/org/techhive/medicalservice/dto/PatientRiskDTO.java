package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRiskDTO {
    private Long appointmentId;
    private String patientId;
    private String title;
    private LocalDate date;
    private LocalTime time;
    private String doctorId;
    private Integer riskScore;
    private String riskLevel;
    private String recommendation;
}
