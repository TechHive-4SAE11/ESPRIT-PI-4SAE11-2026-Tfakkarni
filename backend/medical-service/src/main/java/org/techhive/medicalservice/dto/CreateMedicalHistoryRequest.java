package org.techhive.medicalservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMedicalHistoryRequest {
	@NotNull(message = "Medical folder ID cannot be null")
	@Min(value = 1, message = "Medical folder ID must be positive")
	private Long medicalFolderId;

	@Size(max = 2000, message = "Allergies must not exceed 2000 characters")
	private String allergies;
	
	@Size(max = 2000, message = "Conditions must not exceed 2000 characters")
	private String conditions;
	
	@Size(max = 2000, message = "Surgeries must not exceed 2000 characters")
	private String surgeries;

	@Size(max = 2000, message = "Symptoms must not exceed 2000 characters")
	private String symptoms;

	@Size(max = 2000, message = "Recommended treatment must not exceed 2000 characters")
	private String recommendedTreatment;

	@Size(max = 2000, message = "Family history must not exceed 2000 characters")
	private String familyHistory;
}
