package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.ReportAnalysisResult;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportAnalysisService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public ReportAnalysisResult analyzeMedicalFolder(MedicalFolderDTO medicalFolder) {
        log.info("Analyzing medical folder for patient: {}", medicalFolder.getPatientId());

        String prompt = buildAnalysisPrompt(medicalFolder);
        try {
            ChatClient chatClient = chatClientBuilder.build();
            String response = chatClient.prompt().user(prompt).call().content();
            log.info("AI Analysis response: {}", response);
            return parseAnalysisResult(response);
        } catch (Exception e) {
            log.error("Analysis failed with OpenAI framework", e);
            throw new RuntimeException("Erreur de connexion avec OpenAI: " + e.getMessage(), e);
        }
    }

    private String buildAnalysisPrompt(MedicalFolderDTO folder) {
        return String.format("""
            Analyse ce dossier médical et extrait les informations suivantes au format JSON:

            DOSSIER MÉDICAL:
            - Diagnostic: %s
            - Traitements: %s
            - Évolution: %s
            - Points faibles: %s
            - Recommandations: %s

            Réponds UNIQUEMENT au format JSON suivant (sans texte additionnel):
            {
              "cognitiveLevel": "DEBUTANT",
              "weakTopics": ["sujet1", "sujet2"],
              "recommendedTopics": ["sujet1", "sujet2"],
              "difficultyLevel": 1,
              "customPrompt": "description personnalisée",
              "diagnosis": "diagnostic principal"
            }

            cognitiveLevel peut être: DEBUTANT, INTERMEDIAIRE, ou AVANCE
            difficultyLevel: 1 (Facile), 2 (Moyen), ou 3 (Difficile)
            """,
                folder.getDiagnosis(),
                folder.getTreatments(),
                folder.getEvolution(),
                folder.getWeakPoints(),
                folder.getRecommendations()
        );
    }

    private ReportAnalysisResult parseAnalysisResult(String response) {
        try {
            String cleanResponse = response.trim();
            if (cleanResponse.startsWith("```json")) {
                cleanResponse = cleanResponse.substring(7);
            }
            if (cleanResponse.startsWith("```")) {
                cleanResponse = cleanResponse.substring(3);
            }
            if (cleanResponse.endsWith("```")) {
                cleanResponse = cleanResponse.substring(0, cleanResponse.length() - 3);
            }
            
            return objectMapper.readValue(cleanResponse, ReportAnalysisResult.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            throw new RuntimeException("OpenAI formatting error: " + e.getMessage(), e);
        }
    }
}
