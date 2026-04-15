package org.techhive.trackingservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeetingResponse {
    private Long id;
    private String roomName;
    private String roomUrl;
    private String status;
    private String patientName;
    private String doctorName;
    // Keycloak IDs — needed for rating feature
    private String doctorKeycloakId;
    private String patientKeycloakId;
    private String notes;
    private String aiSummary;
    private String transcript;
    private String transcriptSummaries;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMinutes;
    private LocalDateTime createdAt;
}
