package org.techhive.medicalservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class FolderSpecificStatsDto {
    private long totalDiagnostics;
    private long totalMedicalHistory;
    private Map<String, Long> severityDistribution;
    private double treatmentCoverageRate; // For this specific folder
    private List<MedicationSummary> prescriptions;
    private List<DiagnosticTimelineEntry> timeline;

    @Data
    @Builder
    public static class MedicationSummary {
        private String medicationName;
        private String prescribedAt;
    }

    @Data
    @Builder
    public static class DiagnosticTimelineEntry {
        private String date;
        private String diseaseName;
        private String stage;
    }
}
