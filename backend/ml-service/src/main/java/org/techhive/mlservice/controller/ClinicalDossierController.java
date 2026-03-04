package org.techhive.mlservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.mlservice.dto.ClinicalAnalysisResponse;
import org.techhive.mlservice.dto.DossierAnalysisRequest;
import org.techhive.mlservice.service.GeminiClinicalAnalyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Slf4j
public class ClinicalDossierController {

	private final GeminiClinicalAnalyzer geminiClinicalAnalyzer;

	@PostMapping("/analyze/dossier")
	public ResponseEntity<ClinicalAnalysisResponse> analyzeDossier(@RequestBody DossierAnalysisRequest request) {
		log.info("POST /api/ml/analyze/dossier folderId={}", request.getFolderId());
		ClinicalAnalysisResponse response = geminiClinicalAnalyzer.analyze(request);
		return ResponseEntity.ok(response);
	}
}
