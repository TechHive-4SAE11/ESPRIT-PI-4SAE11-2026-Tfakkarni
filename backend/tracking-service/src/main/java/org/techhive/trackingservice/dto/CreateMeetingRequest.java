package org.techhive.trackingservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMeetingRequest {
    private String patientKeycloakId;
    private String doctorKeycloakId;
    private LocalDateTime scheduledAt;
}
