package org.techhive.medicalservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicalSafetyStatsDto {
    private double treatmentCoverageRate; // % of diagnostics with prescriptions
    private long polypharmacyRiskCount; // Patients with > 5 meds
    private long chronicMonitoringAlerts; // Chronic patients with no recent meds
    private List<MedicationConflictDto> potentialConflicts;
    /** True when {@code medical.analytics.safety-audit.presentation-demo} filled illustrative metrics (no tracking meds). */
    @Builder.Default
    private boolean illustrationData = false;

    /** True when Google Gemini augmented chronic alerts and/or conflicts for this response. */
    @Builder.Default
    private boolean geminiEnriched = false;

    /** Short status for the dashboard (AI on/off, errors). */
    private String geminiNote;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationConflictDto {
        private String patientId;
        /** Optional label for UI (e.g. presentation demo). */
        private String patientDisplayName;
        private String medicationName;
        private String conflictingCondition;
        private String severity; // HIGH, MEDIUM
    }
}
