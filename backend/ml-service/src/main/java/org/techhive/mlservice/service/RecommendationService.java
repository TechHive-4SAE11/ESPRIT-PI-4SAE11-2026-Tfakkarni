package org.techhive.mlservice.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Long> recommendModules(String userId) {
        String promptMsg = buildPrompt(userId);

        Prompt prompt = new Prompt(promptMsg);

        ChatResponse response = chatClient.call(prompt);
        String jsonResponse = response.getResult().getOutput().getContent();

        return parseResponse(jsonResponse);
    }

    private String buildPrompt(String userId) {
        return """
                Recommande des modules de formation pour l'aidant (caregiver) avec l'ID: """ + userId + """

                Analyse son profil et retourne UNIQUEMENT un JSON au format :
                {
                    "recommendedModules": [1, 3, 5]
                }

                Les modules disponibles sont :
                - 1: Comprendre Alzheimer (débutant)
                - 2: Communication avec le patient (débutant)
                - 3: Gestion des médicaments (intermédiaire)
                - 4: Prévention des chutes (intermédiaire)
                - 5: Gestion du stress de l'aidant (avancé)

                Ne retourne que les IDs, pas de texte supplémentaire.
                """;
    }

    private List<Long> parseResponse(String jsonResponse) {
        try {
            JsonNode node = objectMapper.readTree(jsonResponse);
            JsonNode modulesNode = node.get("recommendedModules");

            List<Long> moduleIds = new ArrayList<>();
            if (modulesNode != null && modulesNode.isArray()) {
                for (JsonNode idNode : modulesNode) {
                    moduleIds.add(idNode.asLong());
                }
            }
            return moduleIds.isEmpty() ? getDefaultRecommendations() : moduleIds;
        } catch (Exception e) {
            // Fallback en cas d'erreur
            return getDefaultRecommendations();
        }
    }

    private List<Long> getDefaultRecommendations() {
        return Arrays.asList(1L, 3L, 5L);
    }
}