package org.techhive.medicalservice.service.safety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.techhive.medicalservice.config.GeminiSafetyAuditProperties;

import java.util.Optional;

/**
 * Calls Google Gemini to analyze diagnostics + active medications and suggest chronic alerts / conflicts.
 * Fails soft: returns empty when disabled, missing key, or API error.
 */
@Service
@Slf4j
public class GeminiSafetyAuditService {

    private static final String GEMINI_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final GeminiSafetyAuditProperties props;
    private final ObjectMapper objectMapper;
    private final RestTemplate geminiRestTemplate;

    public GeminiSafetyAuditService(GeminiSafetyAuditProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(20_000);
        f.setReadTimeout(120_000);
        this.geminiRestTemplate = new RestTemplate(f);
    }

    public boolean isEnabled() {
        return props.isEnabled() && props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    /**
     * @param patientDataJson JSON string: { "patients": [ { "patientId", "diagnostics", "activeMedicationsDistinct", "totalActiveMedicationRows" } ] }
     * @return parsed model JSON or empty
     */
    public Optional<JsonNode> analyzePatientPool(String patientDataJson) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        String prompt = buildPrompt(patientDataJson);
        try {
            String url = String.format(GEMINI_URL_TEMPLATE, props.getModel(), props.getApiKey());
            ObjectNode body = objectMapper.createObjectNode();
            ArrayNode contents = body.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);
            ObjectNode gen = objectMapper.createObjectNode();
            gen.put("temperature", 0.15);
            gen.put("responseMimeType", "application/json");
            body.set("generationConfig", gen);
            String jsonBody = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            JsonNode root = geminiRestTemplate.postForObject(url, entity, JsonNode.class);
            if (root == null) {
                return Optional.empty();
            }
            if (root.has("error")) {
                log.warn("Gemini API error payload: {}", root.path("error"));
                return Optional.empty();
            }
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                log.warn("Gemini safety audit: no candidates in response");
                return Optional.empty();
            }
            String text = candidates.get(0).path("content").path("parts").path(0).path("text").asText("");
            if (text.isBlank()) {
                return Optional.empty();
            }
            text = stripMarkdownFence(text);
            JsonNode parsed = objectMapper.readTree(text);
            return Optional.of(parsed);
        } catch (Exception e) {
            log.warn("Gemini safety audit failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String stripMarkdownFence(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) {
                t = t.substring(nl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end > 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }

    private String buildPrompt(String patientDataJson) {
        return """
                You are a clinical decision-support assistant reviewing de-identified EHR extracts for a teaching/demo system.
                Task: compare each patient's DIAGNOSTICS (disease, stage, comorbidities) with their ACTIVE MEDICATIONS from a separate prescriptions system.

                Identify:
                1) chronicAlerts: patients where a serious chronic condition is documented but medications appear absent, clearly insufficient, or inappropriate (brief rationale per row).
                2) conflicts: medication safety issues — drug-drug, duplicate therapy (e.g. same moiety twice), disease-drug, or inappropriate combinations. Include severity HIGH, MEDIUM, or LOW.

                Rules:
                - Use only the data provided; do not invent patient names.
                - Output patientId exactly as given.
                - Prefer empty arrays when nothing applies.
                - Respond with JSON only, no markdown.

                Required JSON shape:
                {"chronicAlerts":[{"patientId":"string","rationale":"string"}],"conflicts":[{"patientId":"string","medicationName":"string","conflictingCondition":"string","severity":"HIGH"}]}

                DATA:
                """
                + patientDataJson;
    }
}
