package org.techhive.trackingservice.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorRankingResponse {
    private String  doctorKeycloakId;
    private String  doctorName;
    private Double  averageRating;     // 1.0 – 5.0
    private Long    totalRatings;
    private Integer rank;              // 1, 2, 3 …
    /** Last 5 reviews for admin display */
    private List<DoctorRatingResponse> recentReviews;
}
