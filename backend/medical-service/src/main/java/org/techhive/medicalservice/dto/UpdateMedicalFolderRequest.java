package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMedicalFolderRequest {
	private String patientId;
	private String doctorId;
	private String bloodType;
	private Double height;
	private Double weight;
}
