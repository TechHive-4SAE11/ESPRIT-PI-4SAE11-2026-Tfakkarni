package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionImpactResponse {
    private String patientKeycloakId;
    private List<PrescriptionImpactPoint> impactTimeline;
    private List<PrescriptionMarker> markers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionImpactPoint {
        private String date;
        private Double avgScore;
        private Double medAdherence;
        private Boolean hasNewPrescription;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrescriptionMarker {
        private String date;
        private String description;
        private Long prescriptionId;
    }
}
