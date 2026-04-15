package org.techhive.trackingservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorRatingResponse {
    private Long          id;
    private Long          meetingId;
    private String        doctorKeycloakId;
    private String        patientKeycloakId;
    private Integer       rating;
    private String        review;
    private String        doctorName;
    private String        patientName;
    private LocalDateTime createdAt;
}
