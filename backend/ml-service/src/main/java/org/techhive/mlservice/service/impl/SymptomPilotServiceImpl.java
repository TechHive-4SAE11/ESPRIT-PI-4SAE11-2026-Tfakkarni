package org.techhive.mlservice.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.techhive.mlservice.dto.SymptomPilotRequest;
import org.techhive.mlservice.dto.SymptomPilotResponse;
import org.techhive.mlservice.service.SymptomPilotService;
import org.techhive.mlservice.service.guardian.MlGuardianDispatchManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SymptomPilotServiceImpl implements SymptomPilotService {

    private static final String HF_URL_TEMPLATE = "https://router.huggingface.co/hf-inference/models/%s";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MlGuardianDispatchManager guardianDispatchManager;

    @Value("${huggingface.api-key:}")
    private String hfApiKey;

    @Value("${huggingface.model:facebook/bart-large-mnli}")
    private String hfModel;

    private static final List<String> CANDIDATE_LABELS = List.of(
            "Heart Attack", "Stroke", "Pulmonary Embolism", "Anaphylaxis",
            "Sepsis", "Pneumothorax", "Panic Attack", "Acid Reflux",
            "Common Cold", "Influenza", "Asthma Attack", "Dehydration",
            "Food Poisoning", "Migraine", "Angina", "Hypoglycemia");

    private static final List<String> CRITICAL_CONDITIONS = List.of(
            "Heart Attack", "Stroke", "Pulmonary Embolism", "Anaphylaxis",
            "Sepsis", "Pneumothorax", "Angina");

    @Override
    public SymptomPilotResponse analyzeSymptoms(SymptomPilotRequest request) {
        if (hfApiKey == null || hfApiKey.isBlank() || hfApiKey.contains("XXXX")) {
            log.warn("Hugging Face API key not set for Symptom Pilot");
            return SymptomPilotResponse.builder()
                    .predictions(new ArrayList<>())
                    .isCriticalAlert(false)
                    .alertMessage("AI Co-Pilot Offline: HF Key Missing")
                    .build();
        }

        String url = String.format(HF_URL_TEMPLATE, hfModel);

        try {
            ObjectNode body = objectMapper.createObjectNode();
            // Add a medical context prefix to help Zero-Shot classification
            String formattedInput = "A medical patient with the following symptoms: " + request.getSymptoms();
            body.put("inputs", formattedInput);

            ObjectNode parameters = body.putObject("parameters");
            ArrayNode labels = parameters.putArray("candidate_labels");
            CANDIDATE_LABELS.forEach(labels::add);
            parameters.put("multi_label", true); // Multi-label gives much better confidence scores for medical matches

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(hfApiKey);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            String responseBody = restTemplate.postForObject(url, entity, String.class);
            SymptomPilotResponse response = parseHFResponse(responseBody);

            // Trigger AI Guardian (Voice Call / WhatsApp Follow-up) in the background
            // We use a dummy patientId for now (could be passed in the request later)
            guardianDispatchManager.processAnalysisResults(response, "P-789");

            return response;
        } catch (Exception e) {
            log.error("Symptom Pilot HF call failed", e);
            return SymptomPilotResponse.builder()
                    .predictions(new ArrayList<>())
                    .isCriticalAlert(false)
                    .alertMessage("Analysis Error: " + e.getMessage())
                    .build();
        }
    }

    private SymptomPilotResponse parseHFResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            List<SymptomPilotResponse.Prediction> allPredictions = new ArrayList<>();

            if (root.isArray()) {
                // HF Router structure: [{"label": "...", "score": 0.99}, ...]
                for (JsonNode item : root) {
                    allPredictions.add(mapToPrediction(item.path("label").asText(), item.path("score").asDouble()));
                }
            } else {
                // Classic Inference API structure: {"labels": ["..."], "scores": [0.99]}
                JsonNode labelsNode = root.path("labels");
                JsonNode scoresNode = root.path("scores");
                for (int i = 0; i < labelsNode.size(); i++) {
                    allPredictions.add(mapToPrediction(labelsNode.get(i).asText(), scoresNode.get(i).asDouble()));
                }
            }

            // Sort by probability descending
            allPredictions.sort((a, b) -> Double.compare(b.getProbability(), a.getProbability()));

            // Take top 3
            List<SymptomPilotResponse.Prediction> predictions = allPredictions.subList(0,
                    Math.min(3, allPredictions.size()));

            // Critical alert logic
            boolean isCritical = false;
            String alertMessage = null;

            if (!predictions.isEmpty()) {
                SymptomPilotResponse.Prediction top = predictions.get(0);
                // Critical alert if top prediction is a critical condition and probability is
                // high
                if (isConditionCritical(top.getCondition()) && top.getProbability() > 0.4) { // Lowered threshold
                                                                                             // slightly for zero-shot
                                                                                             // sensitivity
                    isCritical = true;
                    alertMessage = "CRITICAL WARNING: The detected condition '" + top.getCondition() +
                            "' requires immediate medical attention.";
                }
            }

            return SymptomPilotResponse.builder()
                    .predictions(predictions)
                    .isCriticalAlert(isCritical)
                    .alertMessage(alertMessage)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse HF response", e);
            return SymptomPilotResponse.builder()
                    .predictions(new ArrayList<>())
                    .isCriticalAlert(false)
                    .alertMessage("Parse Error")
                    .build();
        }
    }

    private SymptomPilotResponse.Prediction mapToPrediction(String condition, double probability) {
        String riskLevel = "LOW";
        if (probability > 0.65)
            riskLevel = "HIGH";
        else if (probability > 0.25)
            riskLevel = "MODERATE";

        return SymptomPilotResponse.Prediction.builder()
                .condition(condition)
                .probability(probability)
                .riskLevel(riskLevel)
                .build();
    }

    private boolean isConditionCritical(String condition) {
        return CRITICAL_CONDITIONS.stream()
                .anyMatch(c -> c.equalsIgnoreCase(condition));
    }
}
