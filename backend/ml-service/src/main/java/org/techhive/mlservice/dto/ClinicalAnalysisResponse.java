package org.techhive.mlservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalAnalysisResponse {

	private List<String> differentials;
	private List<String> anomalies;
	private String riskLevel;   // LOW, MEDIUM, HIGH
	private String advice;
	private List<String> contradictions;
}
