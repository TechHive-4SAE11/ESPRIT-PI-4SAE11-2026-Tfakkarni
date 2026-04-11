package org.techhive.medicalservice.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.cloud.openfeign.FeignClient;
import java.util.List;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@FeignClient(name = "tracking-service", path = "/api/tracking")
public interface TrackingServiceClient {

    @GetMapping("/logs/patient/{patientId}/summary")
    TrackingSummaryDTO getPatientTrackingSummary(@PathVariable("patientId") String patientId);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class TrackingSummaryDTO {
        private String patientId;
        private Double medicationCompliance;
        private List<MedicationLogDTO> recentLogs;
        private List<IncidentDTO> recentIncidents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class MedicationLogDTO {
        private String medicationName;
        private String status;
        private java.time.LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class IncidentDTO {
        private String type;
        private String description;
        private java.time.LocalDateTime occurredAt;
    }
}
