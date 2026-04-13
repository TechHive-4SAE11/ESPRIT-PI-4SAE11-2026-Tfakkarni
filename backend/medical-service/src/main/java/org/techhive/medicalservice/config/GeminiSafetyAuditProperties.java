package org.techhive.medicalservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "medical.analytics.safety-audit.gemini")
public class GeminiSafetyAuditProperties {
    /**
     * When true and {@code api-key} is non-empty, the dossier safety audit calls Google Gemini
     * to augment chronic alerts and medication–folder conflicts.
     */
    private boolean enabled = true;
    /** Google AI Studio / Gemini API key ({@code GEMINI_API_KEY}). */
    private String apiKey = "";
    /** Model id for {@code :generateContent} (e.g. gemini-2.0-flash, gemini-1.5-flash). */
    private String model = "gemini-2.0-flash";
    /** Max patients sent in one request (token safety). */
    private int maxPatientsPerRequest = 48;
}
