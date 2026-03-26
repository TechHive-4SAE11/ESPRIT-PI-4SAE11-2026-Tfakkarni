package org.techhive.medicalservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response from ml-service clinical analysis (Gemini) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalAnalysisResult {

	private List<String> differentials;
	private List<String> anomalies;
	private String riskLevel;   // e.g. LOW, MEDIUM, HIGH
	private String advice;
	private List<String> contradictions;
}
