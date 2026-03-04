package org.techhive.mlservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techhive.mlservice.dto.ClinicalAnalysisResponse;
import org.techhive.mlservice.dto.DossierAnalysisRequest;
import org.techhive.mlservice.dto.SymptomPilotRequest;
import org.techhive.mlservice.dto.SymptomPilotResponse;
import org.techhive.mlservice.service.GeminiClinicalAnalyzer;
import org.techhive.mlservice.service.SymptomPilotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Slf4j
public class ClinicalDossierController {

	private final GeminiClinicalAnalyzer geminiClinicalAnalyzer;
	private final SymptomPilotService symptomPilotService;
	private final org.techhive.mlservice.service.guardian.MlGuardianMessagingService messagingService;

	@PostMapping("/analyze/dossier")
	public ResponseEntity<ClinicalAnalysisResponse> analyzeDossier(@RequestBody DossierAnalysisRequest request) {
		log.info("POST /api/ml/analyze/dossier folderId={}", request.getFolderId());
		ClinicalAnalysisResponse response = geminiClinicalAnalyzer.analyze(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/symptom-pilot")
	public ResponseEntity<SymptomPilotResponse> analyzeSymptoms(@RequestBody SymptomPilotRequest request) {
		log.info("POST /api/ml/symptom-pilot symptomsLen={}",
				request.getSymptoms() != null ? request.getSymptoms().length() : 0);
		SymptomPilotResponse response = symptomPilotService.analyzeSymptoms(request);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/test-whatsapp")
	public ResponseEntity<String> testWhatsapp(@RequestBody java.util.Map<String, String> request) {
		String to = request.getOrDefault("to", "+21628706172");
		String contentSid = request.getOrDefault("contentSid", "HXb5b62575e6e4ff6129ad7c8efe1f983e");
		String contentVariables = request.getOrDefault("contentVariables", "{\"1\":\"12/1\",\"2\":\"3pm\"}");

		log.info("Triggering WhatsApp test for {} with template {}", to, contentSid);
		messagingService.sendWhatsAppTemplateMessage(to, contentSid, contentVariables);

		return ResponseEntity.ok("WhatsApp Trial Triggered");
	}
}
