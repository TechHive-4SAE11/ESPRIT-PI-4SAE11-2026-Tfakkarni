package org.techhive.assistantservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.client.dto.EquipmentDTO;
import org.techhive.assistantservice.dto.EquipmentRecommendRequest;
import org.techhive.assistantservice.dto.EquipmentRecommendResponse;
import org.techhive.assistantservice.dto.EquipmentRecommendation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentAIService {

    private final ChatClient.Builder chatClientBuilder;
    private final MedicalServiceClient medicalServiceClient;
    private final ObjectMapper objectMapper;

    /**
     * Recommend equipment based on patient condition and severity.
     * Fetches available equipment from medical-service, then uses OpenAI to rank them.
     */
    public EquipmentRecommendResponse recommendEquipment(EquipmentRecommendRequest request) {
        log.info("Recommending equipment for patient={}, condition={}, severity={}",
                request.getPatientId(), request.getCondition(), request.getSeverity());

        // 1. Fetch available equipment from medical-service
        List<EquipmentDTO> availableEquipment;
        try {
            availableEquipment = medicalServiceClient.getAvailableEquipment();
            log.info("Found {} available equipment items", availableEquipment.size());
        } catch (Exception e) {
            log.warn("Could not fetch equipment from medical-service, using empty list: {}", e.getMessage());
            availableEquipment = new ArrayList<>();
        }

        // 2. Build equipment context for AI
        String equipmentContext = availableEquipment.stream()
                .map(eq -> String.format("- ID: %d, Name: %s, Category: %s, Description: %s, Condition: %s",
                        eq.getId(), eq.getName(), eq.getCategory(),
                        eq.getDescription() != null ? eq.getDescription() : "N/A",
                        eq.getCondition() != null ? eq.getCondition() : "N/A"))
                .collect(Collectors.joining("\n"));

        // 3. Call OpenAI for recommendations
        String aiResponse = callOpenAIForRecommendations(request, equipmentContext);
        log.debug("OpenAI recommendation response: {}", aiResponse);

        // 4. Parse and build response
        return parseRecommendationResponse(request, aiResponse, availableEquipment);
    }

    private String callOpenAIForRecommendations(EquipmentRecommendRequest request, String equipmentContext) {
        String prompt = String.format("""
                You are a medical equipment specialist AI assistant for Alzheimer's/dementia care.
                
                A patient needs equipment recommendations based on the following:
                - Patient ID: %d
                - Medical Condition: %s
                - Severity Level: %s
                
                Available equipment in our inventory:
                %s
                
                Please recommend the TOP 3 most suitable equipment items from the available inventory.
                If the inventory is empty or doesn't have suitable items, provide general recommendations.
                
                For each recommendation, provide:
                1. Equipment ID (from inventory, or -1 if general recommendation)
                2. Equipment name
                3. Category
                4. Detailed justification (why this equipment helps with the condition)
                5. Relevance score (0.0 to 1.0)
                6. Usage instructions specific to the patient's condition
                
                Also provide general medical advice for the condition.
                
                IMPORTANT: Respond ONLY with valid JSON, no additional text.
                Format:
                {
                  "recommendations": [
                    {
                      "equipmentId": 1,
                      "equipmentName": "Equipment Name",
                      "category": "MOBILITY",
                      "justification": "Detailed reason",
                      "relevanceScore": 0.95,
                      "usageInstructions": "How to use"
                    }
                  ],
                  "generalAdvice": "Overall medical advice for the patient"
                }
                """, request.getPatientId(), request.getCondition(), request.getSeverity(),
                equipmentContext.isEmpty() ? "No equipment currently available in inventory." : equipmentContext);

        ChatClient chatClient = chatClientBuilder.build();
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @SuppressWarnings("unchecked")
    private EquipmentRecommendResponse parseRecommendationResponse(
            EquipmentRecommendRequest request, String aiResponse, List<EquipmentDTO> availableEquipment) {
        try {
            String cleaned = cleanJsonResponse(aiResponse);
            Map<String, Object> parsed = objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});

            List<EquipmentRecommendation> recommendations = new ArrayList<>();
            List<Map<String, Object>> recs = (List<Map<String, Object>>) parsed.get("recommendations");
            if (recs != null) {
                for (Map<String, Object> rec : recs) {
                    recommendations.add(EquipmentRecommendation.builder()
                            .equipmentId(toLong(rec.get("equipmentId")))
                            .equipmentName((String) rec.get("equipmentName"))
                            .category((String) rec.get("category"))
                            .justification((String) rec.get("justification"))
                            .relevanceScore(toDouble(rec.get("relevanceScore")))
                            .usageInstructions((String) rec.get("usageInstructions"))
                            .build());
                }
            }

            return EquipmentRecommendResponse.builder()
                    .patientId(request.getPatientId())
                    .condition(request.getCondition())
                    .severity(request.getSeverity())
                    .recommendations(recommendations)
                    .generalAdvice((String) parsed.get("generalAdvice"))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI recommendation response: {}", e.getMessage());
            return EquipmentRecommendResponse.builder()
                    .patientId(request.getPatientId())
                    .condition(request.getCondition())
                    .severity(request.getSeverity())
                    .recommendations(new ArrayList<>())
                    .generalAdvice("Unable to generate AI recommendations. Please consult your healthcare provider.")
                    .build();
        }
    }

    private String cleanJsonResponse(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
        else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
        if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
        return cleaned.trim();
    }

    private Long toLong(Object value) {
        if (value == null) return -1L;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return -1L; }
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (NumberFormatException e) { return 0.0; }
    }
}
