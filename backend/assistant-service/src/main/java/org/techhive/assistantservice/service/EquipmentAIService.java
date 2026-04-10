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

        try {
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("OpenAI API call failed ({}), using fallback recommendations", e.getMessage());
            return generateFallbackRecommendations(request);
        }
    }

    /**
     * Fallback: generates realistic equipment recommendation JSON without OpenAI.
     */
    private String generateFallbackRecommendations(EquipmentRecommendRequest request) {
        String condition = request.getCondition();
        String severity = request.getSeverity();

        // Condition-specific recommendations
        String recs = switch (condition) {
            case "MOBILITY" -> """
                {
                  "recommendations": [
                    {"equipmentId": -1, "equipmentName": "Electric Wheelchair", "category": "MOBILITY", "justification": "Essential for patients with reduced mobility. Facilitates daily movement and reduces fatigue significantly.", "relevanceScore": 0.95, "usageInstructions": "Use daily for transportation. Recharge the battery every night. Adjust the footrests for patient comfort."},
                    {"equipmentId": -1, "equipmentName": "Foldable Walker", "category": "MOBILITY", "justification": "Provides stable walking support. Lightweight and easy to store, perfect for indoor use.", "relevanceScore": 0.88, "usageInstructions": "Adjust height to hip level. Patient should move the walker forward then take a step. Always use on flat surfaces."},
                    {"equipmentId": -1, "equipmentName": "Ergonomic Walking Cane", "category": "MOBILITY", "justification": "For mild cases requiring occasional support. Ergonomic handle reduces wrist pain.", "relevanceScore": 0.75, "usageInstructions": "Hold in the hand opposite to the weakened side. Move the cane forward at the same time as the weak foot."}
                  ],
                  "generalAdvice": "For a mobility condition of %s severity, it is recommended to start with rehabilitation exercises supervised by a physiotherapist. Equipment should be progressively adapted based on the patient's evolution."
                }
                """;
            case "RESPIRATORY" -> """
                {
                  "recommendations": [
                    {"equipmentId": -1, "equipmentName": "Portable Oxygen Concentrator", "category": "RESPIRATORY", "justification": "Provides a constant oxygen supply. Portable and quiet for home use.", "relevanceScore": 0.97, "usageInstructions": "Set the flow rate according to the medical prescription. Clean the filter weekly. Check oxygen levels regularly."},
                    {"equipmentId": -1, "equipmentName": "Ultrasonic Nebulizer", "category": "RESPIRATORY", "justification": "Enables efficient medication delivery via inhalation. Ideal for bronchodilator treatments.", "relevanceScore": 0.85, "usageInstructions": "Use with prescribed medications. Clean after each use. Sessions of 10-15 minutes, 2 to 3 times daily."},
                    {"equipmentId": -1, "equipmentName": "Pulse Oximeter", "category": "RESPIRATORY", "justification": "Continuous monitoring of oxygen saturation. Alerts when levels drop dangerously.", "relevanceScore": 0.80, "usageInstructions": "Place on finger for reading. Monitor that SpO2 stays above 95%%. Consult a doctor if it drops below 90%%."}
                  ],
                  "generalAdvice": "For a respiratory condition of %s severity, regular monitoring of oxygen saturation is essential. Maintain a well-ventilated environment and avoid respiratory irritants."
                }
                """;
            default -> """
                {
                  "recommendations": [
                    {"equipmentId": -1, "equipmentName": "Medical ID Bracelet", "category": "DAILY_LIVING", "justification": "Essential for Alzheimer's patients. Contains emergency information and patient identity.", "relevanceScore": 0.92, "usageInstructions": "Wear at all times. Verify that information is up to date. Include emergency number and allergies."},
                    {"equipmentId": -1, "equipmentName": "Electronic Pill Dispenser with Alarm", "category": "DAILY_LIVING", "justification": "Ensures correct medication intake through audio and visual reminders.", "relevanceScore": 0.88, "usageInstructions": "Program the schedule for each medication. Refill compartments weekly. Check daily that doses are taken."},
                    {"equipmentId": -1, "equipmentName": "Connected Fall Detector", "category": "DAILY_LIVING", "justification": "Automatically detects falls and alerts caregivers. Crucial for patient safety when living alone.", "relevanceScore": 0.85, "usageInstructions": "Wear around neck or wrist. Test the alert system monthly. Ensure emergency contacts are up to date."}
                  ],
                  "generalAdvice": "For a %s condition of %s severity, regular medical follow-up is recommended. Adapt the patient's environment to maximize safety and autonomy."
                }
                """;
        };

        // Format severity into the advice string
        if (condition.equals("MOBILITY") || condition.equals("RESPIRATORY")) {
            return String.format(recs, severity);
        } else {
            return String.format(recs, condition, severity);
        }
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
