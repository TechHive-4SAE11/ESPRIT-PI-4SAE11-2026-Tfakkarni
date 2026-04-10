package org.techhive.mlservice.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.mlservice.dto.StressAnalysisDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StressDetectionService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StressAnalysisDTO analyzeStress(String userId) {
        String promptMsg = buildPrompt(userId);

        Prompt prompt = new Prompt(promptMsg);

        ChatResponse response = chatClient.call(prompt);
        String jsonResponse = response.getResult().getOutput().getContent();

        return parseResponse(jsonResponse);
    }

    private String buildPrompt(String userId) {
        return """
                Analyse le stress de l'aidant (caregiver) avec l'ID: """ + userId + """

                Retourne UNIQUEMENT un JSON valide sans texte supplémentaire, au format exact suivant :
                {
                    "stressLevel": "LOW/MEDIUM/HIGH",
                    "factors": ["facteur1", "facteur2"],
                    "recommendation": "conseil personnalisé"
                }
                """;
    }

    private StressAnalysisDTO parseResponse(String jsonResponse) {
        try {
            JsonNode node = objectMapper.readTree(jsonResponse);
            String stressLevel = node.get("stressLevel").asText();

            List<String> factors = new ArrayList<>();
            JsonNode factorsNode = node.get("factors");
            if (factorsNode != null && factorsNode.isArray()) {
                for (JsonNode factor : factorsNode) {
                    factors.add(factor.asText());
                }
            }

            String recommendation = node.get("recommendation").asText();

            return new StressAnalysisDTO(stressLevel, factors, recommendation);
        } catch (Exception e) {
            // Fallback en cas d'erreur de parsing
            return new StressAnalysisDTO(
                    "MEDIUM",
                    List.of("Données insuffisantes pour l'analyse"),
                    "Continuez à utiliser le chatbot pour un suivi personnalisé");
        }
    }
}