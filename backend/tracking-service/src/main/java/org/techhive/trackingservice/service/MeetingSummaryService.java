package org.techhive.trackingservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI Summary Service using Groq API (free tier, Llama 3.3 70B).
 *
 * Groq uses the OpenAI-compatible /chat/completions format.
 * Free tier: 14,400 requests/day, 6,000 tokens/minute.
 * Sign up at: https://console.groq.com
 */
@Service
@Slf4j
public class MeetingSummaryService {

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final RestTemplate restTemplate;

    public MeetingSummaryService(
            @Value("${claude.api-key}") String apiKey,
            @Value("${claude.api-url}") String apiUrl,
            @Value("${claude.model}") String model,
            @Qualifier("plainRestTemplate") RestTemplate restTemplate) {
        this.apiKey      = apiKey;
        this.apiUrl      = apiUrl;
        this.model       = model;
        this.restTemplate = restTemplate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TEST — GET /api/meetings/test-claude
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> testClaudeConnection() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider",  apiUrl.contains("groq") ? "Groq (free)" : "Anthropic Claude");
        result.put("model",     model);
        result.put("url",       apiUrl);
        result.put("keyPrefix", apiKey != null && apiKey.length() > 12
                ? apiKey.substring(0, 12) + "***" : "MISSING");

        try {
            HttpHeaders headers = buildHeaders();

            Map<String, Object> body = buildRequestBody("Reply with exactly: OK", 5);

            ResponseEntity<Map> resp = restTemplate.exchange(
                    apiUrl, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);

            result.put("httpStatus", resp.getStatusCode().toString());
            result.put("status",     "✅ AI API works! Response: " + extractText(resp.getBody()));

        } catch (HttpClientErrorException e) {
            result.put("status", "❌ HTTP " + e.getStatusCode());
            result.put("error",  e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            result.put("status", "❌ Server error " + e.getStatusCode());
            result.put("error",  e.getResponseBodyAsString());
        } catch (Exception e) {
            result.put("status", "❌ " + e.getClass().getSimpleName());
            result.put("error",  e.getMessage());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN — Generate meeting summary
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public String generateSummary(String notes, String patientName,
                                  String doctorName, int durationMinutes) {

        if (notes == null || notes.trim().isEmpty()) {
            return "Aucune note prise pendant cette réunion.";
        }

        log.info("━━━ AI Summary API call ━━━ provider={} model={}",
                apiUrl.contains("groq") ? "Groq" : "Anthropic", model);

        try {
            String prompt = buildPrompt(notes, patientName, doctorName, durationMinutes);
            HttpHeaders headers = buildHeaders();
            Map<String, Object> body = buildRequestBody(prompt, 1024);

            ResponseEntity<Map> resp = restTemplate.exchange(
                    apiUrl, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);

            log.info("AI API response: HTTP {}", resp.getStatusCode());

            String summary = extractText(resp.getBody());
            if (summary != null && !summary.isBlank()) {
                log.info("✅ AI summary generated ({} chars) for patient '{}'",
                        summary.length(), patientName);
                return summary.trim();
            }

            log.warn("AI API returned empty content. Full response: {}", resp.getBody());
            return generateLocalFallback(notes, patientName, doctorName, durationMinutes);

        } catch (HttpClientErrorException e) {
            log.error("❌ AI API 4xx: HTTP {} → {}", e.getStatusCode(), e.getResponseBodyAsString());
            return generateLocalFallback(notes, patientName, doctorName, durationMinutes);
        } catch (HttpServerErrorException e) {
            log.error("❌ AI API 5xx: HTTP {} → {}", e.getStatusCode(), e.getResponseBodyAsString());
            return generateLocalFallback(notes, patientName, doctorName, durationMinutes);
        } catch (ResourceAccessException e) {
            log.error("❌ AI API network/timeout: {}", e.getMessage());
            return generateLocalFallback(notes, patientName, doctorName, durationMinutes);
        } catch (Exception e) {
            log.error("❌ AI API unexpected: {} — {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return generateLocalFallback(notes, patientName, doctorName, durationMinutes);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARTIAL SUMMARY — Generate a mini-summary for a transcript segment
    // ─────────────────────────────────────────────────────────────────────────

    public String generatePartialSummary(String transcriptChunk, String segmentLabel,
                                         String patientName, String doctorName) {
        if (transcriptChunk == null || transcriptChunk.trim().isEmpty()) {
            return "Aucune parole détectée dans ce segment.";
        }
        log.info("━━━ Partial summary ({}) — Groq model={}", segmentLabel, model);
        try {
            String prompt = buildPartialSummaryPrompt(transcriptChunk, segmentLabel, patientName, doctorName);
            HttpHeaders headers = buildHeaders();
            Map<String, Object> body = buildRequestBody(prompt, 512);

            ResponseEntity<Map> resp = restTemplate.exchange(
                    apiUrl, HttpMethod.POST,
                    new HttpEntity<>(body, headers), Map.class);

            String text = extractText(resp.getBody());
            if (text != null && !text.isBlank()) {
                log.info("✅ Partial summary generated ({} chars)", text.length());
                return text.trim();
            }
            return "Résumé partiel non disponible.";
        } catch (Exception e) {
            log.warn("⚠️ Partial summary failed ({}): {}", segmentLabel, e.getMessage());
            return "Résumé partiel non disponible — API indisponible.";
        }
    }

    private String buildPartialSummaryPrompt(String chunk, String segmentLabel,
                                              String patientName, String doctorName) {
        return "Tu es un assistant médical. Génère un résumé court (3-5 lignes) en français \n"
             + "de ce segment de réunion médicale entre Dr. " + doctorName
             + " et le patient " + patientName + ".\n\n"
             + "Segment : " + segmentLabel + "\n"
             + "Transcription :\n" + chunk + "\n\n"
             + "Résumé concis des points clés (pas de titres, juste du texte) :";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    private HttpHeaders buildHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(apiKey);  // "Authorization: Bearer <key>"
        return h;
    }

    private Map<String, Object> buildRequestBody(String userMessage, int maxTokens) {
        // OpenAI /chat/completions format (used by Groq, OpenAI, and many others)
        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role",    "system");
        systemMsg.put("content", "Tu es un assistant médical spécialisé dans le suivi des patients Alzheimer. "
                               + "Génère des résumés médicaux structurés, concis et professionnels en français.");

        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role",    "user");
        userMsg.put("content", userMessage);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model",       model);
        body.put("messages",    List.of(systemMsg, userMsg));
        body.put("max_tokens",  maxTokens);
        body.put("temperature", 0.3);
        return body;
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> responseBody) {
        if (responseBody == null) return null;

        // OpenAI format: choices[0].message.content
        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) responseBody.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message != null) {
                Object content = message.get("content");
                return content != null ? content.toString() : null;
            }
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt
    // ─────────────────────────────────────────────────────────────────────────

    private String buildPrompt(String notes, String patientName,
                               String doctorName, int durationMinutes) {
        return "Génère un résumé structuré et professionnel en français de cette réunion médicale.\n\n"
             + "Participants :\n"
             + "- Médecin : Dr. " + doctorName + "\n"
             + "- Patient : " + patientName + "\n"
             + "- Durée : " + durationMinutes + " minutes\n\n"
             + "Notes de la réunion :\n"
             + notes + "\n\n"
             + "Génère un résumé avec EXACTEMENT ces 4 sections (utilise ## pour les titres) :\n\n"
             + "## Résumé de la réunion\n"
             + "[2-3 phrases résumant les points principaux]\n\n"
             + "## Points médicaux discutés\n"
             + "[Liste des sujets médicaux abordés]\n\n"
             + "## Décisions et actions\n"
             + "[Ce qui a été décidé, prescriptions, examens]\n\n"
             + "## Suivi recommandé\n"
             + "[Prochains rendez-vous, recommandations]\n\n"
             + "Sois concis et professionnel. Ne commence pas par des formules de politesse.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Local fallback (when AI API is unavailable)
    // ─────────────────────────────────────────────────────────────────────────

    private String generateLocalFallback(String notes, String patientName,
                                         String doctorName, int durationMinutes) {
        log.warn("⚠️  Using LOCAL fallback summary (AI API unavailable)");

        String[] lines = notes.split("\\n");
        List<String> keyPoints = new ArrayList<>();
        for (String line : lines) {
            String t = line.trim();
            if (!t.isEmpty() && t.length() > 5) {
                keyPoints.add("- " + t);
                if (keyPoints.size() >= 6) break;
            }
        }
        String pointsList = keyPoints.isEmpty()
                ? "- Voir les notes de la réunion"
                : String.join("\n", keyPoints);

        return "## Résumé de la réunion\n"
             + "Réunion médicale entre Dr. " + doctorName + " et le patient "
             + patientName + ", d'une durée de " + durationMinutes + " minutes.\n\n"
             + "## Points abordés\n" + pointsList + "\n\n"
             + "## Note\n"
             + "⚠️ Ce résumé a été généré sans IA (API indisponible). "
             + "Consultez les notes complètes pour tous les détails.";
    }
}
