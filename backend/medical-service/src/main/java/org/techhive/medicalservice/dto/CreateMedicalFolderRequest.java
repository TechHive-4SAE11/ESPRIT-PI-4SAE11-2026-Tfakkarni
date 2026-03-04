package org.techhive.medicalservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMedicalFolderRequest {

	@NotNull(message = "Patient ID cannot be null")
	@NotBlank(message = "Patient ID cannot be blank")
	@Size(min = 1, max = 255, message = "Patient ID must be between 1 and 255 characters")
	private String patientId;

	// doctorId is extracted from JWT token, not from request body
	private String doctorId;

	@Size(max = 10, message = "Blood type must not exceed 10 characters")
	private String bloodType;

	private Double height;

	private Double weight;
}
