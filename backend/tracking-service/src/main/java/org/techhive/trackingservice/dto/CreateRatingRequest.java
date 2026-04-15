package org.techhive.trackingservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateRatingRequest {
    private Long   meetingId;
    private String doctorKeycloakId;
    private String patientKeycloakId;
    private Integer rating;   // 1–5
    private String  review;   // required when rating <= 3
}
