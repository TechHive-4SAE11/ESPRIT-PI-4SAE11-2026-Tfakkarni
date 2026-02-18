package org.techhive.medicalservice.dto;

import jakarta.validation.constraints.NotNull;
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
	private Long medicalFolderId;

	private String allergies;
	private String conditions;
	private String surgeries;
}
