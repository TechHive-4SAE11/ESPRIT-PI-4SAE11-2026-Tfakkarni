package org.techhive.trackingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class MeetingSummaryService {

    private final String claudeApiKey;
    private final String claudeApiUrl;
    private final String claudeModel;
    private final RestTemplate restTemplate;

    public MeetingSummaryService(
            @Value("${claude.api-key}") String claudeApiKey,
            @Value("${claude.api-url}") String claudeApiUrl,
            @Value("${claude.model}") String claudeModel,
            @Qualifier("plainRestTemplate") RestTemplate restTemplate) {
        this.claudeApiKey = claudeApiKey;
        this.claudeApiUrl = claudeApiUrl;
        this.claudeModel = claudeModel;
        this.restTemplate = restTemplate;
    }

    /**
     * Generate an AI summary of the meeting notes using Claude API.
     */
    @SuppressWarnings("unchecked")
    public String generateSummary(String notes, String patientName, String doctorName, int durationMinutes) {
        if (notes == null || notes.trim().isEmpty()) {
            return "Aucune note prise pendant cette réunion.";
        }

        try {
            String prompt = String.format("""
                    Tu es un assistant médical spécialisé dans le suivi des patients Alzheimer.
                    
                    Génère un résumé structuré et professionnel de cette réunion médicale en français.
                    
                    Participants :
                    - Médecin : Dr. %s
                    - Concernant le patient : %s
                    - Durée : %d minutes
                    
                    Notes de la réunion :
                    %s
                    
                    Génère un résumé avec exactement ces 4 sections :
                    
                    ## Résumé de la réunion
                    [2-3 phrases résumant les points principaux]
                    
                    ## Points médicaux discutés
                    [Liste des sujets médicaux abordés]
                    
                    ## Décisions et actions
                    [Ce qui a été décidé, prescriptions modifiées, examens demandés]
                    
                    ## Suivi recommandé
                    [Prochains rendez-vous, points de surveillance, recommandations]
                    
                    Sois concis, professionnel et cliniquement pertinent.
                    """, doctorName, patientName, durationMinutes, notes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", claudeApiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", claudeModel);
            body.put("max_tokens", 1000);
            body.put("messages", List.of(message));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    claudeApiUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
                if (!content.isEmpty()) {
                    String summary = content.get(0).get("text").toString();
                    log.info("AI summary generated successfully for meeting with patient '{}'", patientName);
                    return summary;
                }
            }

            log.warn("Empty response from Claude API");
            return "Résumé non disponible — réponse vide de l'API.";

        } catch (Exception e) {
            log.error("Failed to generate AI summary: {}", e.getMessage());
            return "Résumé non disponible — erreur lors de la génération.";
        }
    }
}
