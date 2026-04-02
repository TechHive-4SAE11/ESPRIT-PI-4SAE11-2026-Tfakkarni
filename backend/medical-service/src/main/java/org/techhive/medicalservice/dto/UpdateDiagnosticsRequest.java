package org.techhive.medicalservice.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDiagnosticsRequest {
	private String diseaseName;
	private String stage;
	private String comorbidities;
	private LocalDateTime diagnosisDate;
}
