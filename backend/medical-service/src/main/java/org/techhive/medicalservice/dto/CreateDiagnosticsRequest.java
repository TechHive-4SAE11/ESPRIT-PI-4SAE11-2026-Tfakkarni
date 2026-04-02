package org.techhive.medicalservice.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
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
	@Min(value = 1, message = "Medical folder ID must be positive")
	private Long medicalFolderId;

	@NotNull(message = "Disease name cannot be null")
	@NotBlank(message = "Disease name cannot be blank")
	@Size(min = 2, max = 255, message = "Disease name must be between 2 and 255 characters")
	private String diseaseName;

	@Size(max = 100, message = "Stage must not exceed 100 characters")
	private String stage;
	
	@Size(max = 1000, message = "Comorbidities must not exceed 1000 characters")
	private String comorbidities;

	@NotNull(message = "Diagnosis date cannot be null")
	@PastOrPresent(message = "Diagnosis date cannot be in the future")
	private LocalDateTime diagnosisDate;
}
