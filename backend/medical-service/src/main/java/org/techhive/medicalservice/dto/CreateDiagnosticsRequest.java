package org.techhive.medicalservice.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDiagnosticsRequest {
	@NotNull(message = "Medical folder ID cannot be null")
	private Long medicalFolderId;

	@NotNull(message = "Disease name cannot be null")
	private String diseaseName;

	private String stage;
	private String comorbidities;

	@NotNull(message = "Diagnosis date cannot be null")
	private LocalDateTime diagnosisDate;
}
