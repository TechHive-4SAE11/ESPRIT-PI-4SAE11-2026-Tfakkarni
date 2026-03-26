package org.techhive.mlservice.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.techhive.mlservice.dto.ClinicalAnalysisResponse;
import org.techhive.mlservice.dto.DossierAnalysisRequest;
import org.techhive.mlservice.service.GeminiClinicalAnalyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiClinicalAnalyzerImpl implements GeminiClinicalAnalyzer {

	private static final String GEMINI_URL_TEMPLATE =
		"https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${gemini.api-key:}")
	private String apiKey;

	@Value("${gemini.model:gemini-1.5-flash}")
	private String model;

	@Override
	public ClinicalAnalysisResponse analyze(DossierAnalysisRequest dossier) {
		if (apiKey == null || apiKey.isBlank()) {
			log.warn("Gemini API key not set; returning placeholder analysis");
			return placeholderResponse();
		}
		String prompt = buildPrompt(dossier);
		String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);
		ObjectNode body = objectMapper.createObjectNode();
		ArrayNode contents = body.putArray("contents");
		ObjectNode content = contents.addObject();
		ArrayNode parts = content.putArray("parts");
		parts.addObject().put("text", prompt);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

		try {
			String responseBody = restTemplate.postForObject(url, entity, String.class);
			return parseGeminiResponse(responseBody);
		} catch (Exception e) {
			log.error("Gemini API call failed", e);
			return errorResponse(e.getMessage());
		}
	}

	private String buildPrompt(DossierAnalysisRequest d) {
		StringBuilder sb = new StringBuilder();
		sb.append("You are a clinical decision support assistant. Analyze this medical dossier and respond ONLY with a valid JSON object (no markdown, no code block). Use this exact structure:\n");
		sb.append("{\n");
		sb.append("  \"differentials\": [\"string\"],\n");
		sb.append("  \"anomalies\": [\"string\"],\n");
		sb.append("  \"riskLevel\": \"LOW\" or \"MEDIUM\" or \"HIGH\",\n");
		sb.append("  \"advice\": \"string\",\n");
		sb.append("  \"contradictions\": [\"string\"]\n");
		sb.append("}\n\n");
		sb.append("Context: Patient ID ").append(d.getPatientId()).append(", Folder ID ").append(d.getFolderId()).append(".\n\n");
		sb.append("Diagnostics:\n");
		if (d.getDiagnostics() != null) {
			for (var diag : d.getDiagnostics()) {
				sb.append("- ").append(diag.getDiseaseName());
				if (diag.getStage() != null && !diag.getStage().isBlank()) sb.append(" ").append(diag.getStage());
				if (diag.getComorbidities() != null && !diag.getComorbidities().isBlank()) sb.append("; comorbidities: ").append(diag.getComorbidities());
				sb.append("; date: ").append(diag.getDiagnosisDate()).append("\n");
			}
		} else {
			sb.append("(none)\n");
		}
		sb.append("\nMedical history (allergies, conditions, surgeries):\n");
		if (d.getMedicalHistory() != null) {
			for (var h : d.getMedicalHistory()) {
				sb.append("- allergies: ").append(nullToEmpty(h.getAllergies()));
				sb.append("; conditions: ").append(nullToEmpty(h.getConditions()));
				sb.append("; surgeries: ").append(nullToEmpty(h.getSurgeries())).append("\n");
			}
		} else {
			sb.append("(none)\n");
		}
		sb.append("\nProvide: (1) differentials: other conditions to consider given this profile. ");
		sb.append("(2) anomalies: e.g. rapid stage progression. (3) riskLevel. (4) advice: short clinical advice. ");
		sb.append("(5) contradictions: e.g. allergy vs prescribed treatment. Reply with ONLY the JSON object.");
		return sb.toString();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private ClinicalAnalysisResponse parseGeminiResponse(String responseBody) {
		try {
			JsonNode root = objectMapper.readTree(responseBody);
			JsonNode candidates = root.path("candidates");
			if (candidates.isEmpty() || !candidates.get(0).path("content").path("parts").isArray()) {
				return errorResponse("No content in Gemini response");
			}
			String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
			// Strip markdown code block if present
			text = text.trim();
			if (text.startsWith("```")) {
				int start = text.indexOf("\n") + 1;
				int end = text.lastIndexOf("```");
				text = (end > start) ? text.substring(start, end).trim() : text.substring(start).trim();
			}
			return objectMapper.readValue(text, ClinicalAnalysisResponse.class);
		} catch (Exception e) {
			log.error("Failed to parse Gemini response", e);
			return errorResponse("Parse error: " + e.getMessage());
		}
	}

	private ClinicalAnalysisResponse placeholderResponse() {
		return ClinicalAnalysisResponse.builder()
			.differentials(List.of("Add GEMINI_API_KEY to enable AI analysis"))
			.anomalies(Collections.emptyList())
			.riskLevel("LOW")
			.advice("Configure gemini.api-key in ml-service to get real clinical insights.")
			.contradictions(Collections.emptyList())
			.build();
	}

	private ClinicalAnalysisResponse errorResponse(String message) {
		return ClinicalAnalysisResponse.builder()
			.differentials(new ArrayList<>())
			.anomalies(List.of("Analysis failed: " + message))
			.riskLevel("LOW")
			.advice("Retry later or check ml-service logs.")
			.contradictions(new ArrayList<>())
			.build();
	}
}
