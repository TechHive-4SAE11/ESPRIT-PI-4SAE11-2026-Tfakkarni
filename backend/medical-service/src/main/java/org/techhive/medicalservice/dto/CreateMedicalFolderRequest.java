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
public class CreateMedicalFolderRequest {

	@NotNull(message = "Patient ID cannot be null")
	private String patientId;

	@NotNull(message = "Doctor ID cannot be null")
	private String doctorId;
}
