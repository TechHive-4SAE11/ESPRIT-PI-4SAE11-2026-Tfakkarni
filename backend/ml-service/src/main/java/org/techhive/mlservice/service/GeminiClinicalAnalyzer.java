package org.techhive.mlservice.service;

import org.techhive.mlservice.dto.ClinicalAnalysisResponse;
import org.techhive.mlservice.dto.DossierAnalysisRequest;

public interface GeminiClinicalAnalyzer {

	/**
	 * Analyze a medical dossier and return structured clinical insights.
	 */
	ClinicalAnalysisResponse analyze(DossierAnalysisRequest dossier);
}
