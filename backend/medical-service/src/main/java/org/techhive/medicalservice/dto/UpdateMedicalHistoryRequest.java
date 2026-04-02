package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMedicalHistoryRequest {
	private String allergies;
	private String conditions;
	private String surgeries;
	private String symptoms;
	private String recommendedTreatment;
	private String familyHistory;
}
