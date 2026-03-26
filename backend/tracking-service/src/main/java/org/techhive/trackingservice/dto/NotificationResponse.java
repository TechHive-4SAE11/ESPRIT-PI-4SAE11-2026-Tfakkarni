package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String doctorKeycloakId;
    private String patientKeycloakId;
    private String patientName;
    private String incidentType;
    private String severity;
    private String description;
    private String location;
    private String actionTaken;
    private String occurredAt;
    private String logDate;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
