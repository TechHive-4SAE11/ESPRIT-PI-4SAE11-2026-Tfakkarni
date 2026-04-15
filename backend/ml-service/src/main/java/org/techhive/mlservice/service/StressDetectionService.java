package org.techhive.mlservice.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.mlservice.dto.StressAnalysisDTO;
import org.techhive.mlservice.entity.CaregiverStressHistory;
import org.techhive.mlservice.repository.CaregiverStressHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class StressDetectionService {

    private final ChatClient chatClient;
    private final CaregiverStressHistoryRepository stressHistoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public StressAnalysisDTO analyzeStress(String userId) {
        StressAnalysisDTO result = null;

        // Tentative d'appel à l'IA
        try {
            String promptMsg = buildPrompt(userId);
            Prompt prompt = new Prompt(promptMsg);
            ChatResponse response = chatClient.call(prompt);
            String jsonResponse = response.getResult().getOutput().getContent();
            result = parseResponse(jsonResponse);
            System.out.println("✅ IA utilisée avec succès pour l'utilisateur: " + userId);
        } catch (Exception e) {
            System.err.println("⚠️ Erreur IA, utilisation du mock pour l'utilisateur: " + userId);
            System.err.println("Erreur: " + e.getMessage());
            result = getMockResponse();
        }

        // STOCKAGE EN BASE DE DONNÉES
        saveStressHistory(userId, result);

        return result;
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
            System.err.println("Erreur parsing JSON, utilisation du mock: " + e.getMessage());
            return getMockResponse();
        }
    }

    private StressAnalysisDTO getMockResponse() {
        String[] levels = {"LOW", "MEDIUM", "HIGH"};
        String stressLevel = levels[random.nextInt(3)];

        List<String> factors;
        String recommendation;

        if (stressLevel.equals("HIGH")) {
            factors = Arrays.asList("Fatigue accumulée", "Manque de sommeil", "Charge mentale élevée");
            recommendation = "⚠️ Alerte : Consultez un professionnel. Appelez le 3114 si nécessaire.";
        } else if (stressLevel.equals("MEDIUM")) {
            factors = Arrays.asList("Stress modéré", "Quelques difficultés");
            recommendation = "Votre stress est modéré. Prenez 10 minutes de pause par jour.";
        } else {
            factors = Arrays.asList("Bien-être général", "Stress contrôlé");
            recommendation = "Vous gérez bien votre stress. Continuez ainsi !";
        }

        return new StressAnalysisDTO(stressLevel, factors, recommendation);
    }

    private void saveStressHistory(String userId, StressAnalysisDTO stress) {
        try {
            String factorsJson = objectMapper.writeValueAsString(stress.getFactors());

            CaregiverStressHistory history = CaregiverStressHistory.builder()
                    .caregiverId(userId)
                    .stressScore(convertLevelToScore(stress.getStressLevel()))
                    .stressLevel(stress.getStressLevel())
                    .factors(factorsJson)
                    .recommendation(stress.getRecommendation())
                    .triggeredBy("CHAT")
                    .createdAt(LocalDateTime.now())
                    .build();

            stressHistoryRepository.save(history);
            System.out.println("✅ Stress sauvegardé pour l'utilisateur: " + userId);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la sauvegarde du stress: " + e.getMessage());
        }
    }

    private int convertLevelToScore(String level) {
        switch (level) {
            case "LOW": return 25;
            case "MEDIUM": return 65;
            case "HIGH": return 85;
            default: return 50;
        }
    }

    // Récupérer l'historique du stress
    public List<CaregiverStressHistory> getStressHistory(String userId, int days) {
        return stressHistoryRepository.findByCaregiverIdAndCreatedAtAfterOrderByCreatedAtAsc(
                userId, LocalDateTime.now().minusDays(days));
    }

    // Récupérer le dernier score
    public CaregiverStressHistory getLatestStress(String userId) {
        return stressHistoryRepository.findTopByCaregiverIdOrderByCreatedAtDesc(userId).orElse(null);
    }

    // Récupérer la tendance du stress
    public String getStressTrend(String userId) {
        List<CaregiverStressHistory> history = stressHistoryRepository
                .findByCaregiverIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, LocalDateTime.now().minusDays(30));

        if (history.size() < 2) return "INSUFFISANT";

        int oldScore = history.get(0).getStressScore();
        int newScore = history.get(history.size() - 1).getStressScore();

        if (newScore < oldScore - 10) return "📉 AMÉLIORATION";
        if (newScore > oldScore + 10) return "📈 DÉGRADATION";
        return "➡️ STABLE";
    }

    // Ajouter manuellement une entrée de stress (pour tests)
    public CaregiverStressHistory saveStressHistory(CaregiverStressHistory stress) {
        if (stress.getCreatedAt() == null) {
            stress.setCreatedAt(LocalDateTime.now());
        }
        return stressHistoryRepository.save(stress);
    }
}