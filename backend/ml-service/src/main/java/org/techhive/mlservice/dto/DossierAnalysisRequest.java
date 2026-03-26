package org.techhive.mlservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DossierAnalysisRequest {

	private Long folderId;
	private String patientId;
	private String doctorId;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime folderCreatedAt;

	private List<DiagnosticsSummary> diagnostics;
	private List<MedicalHistorySummary> medicalHistory;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DiagnosticsSummary {
		private Long id;
		private String diseaseName;
		private String stage;
		private String comorbidities;

		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		private LocalDateTime diagnosisDate;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MedicalHistorySummary {
		private Long id;
		private String allergies;
		private String conditions;
		private String surgeries;

		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
		private LocalDateTime createdAt;
	}
}
