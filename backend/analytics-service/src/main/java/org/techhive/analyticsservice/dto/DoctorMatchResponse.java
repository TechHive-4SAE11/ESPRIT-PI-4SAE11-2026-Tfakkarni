package org.techhive.analyticsservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorMatchResponse {
    private String doctorKeycloakId;
    private String doctorName;

    /** Composite matching score 0–100 */
    private double matchScore;

    /** Average patient star rating (1.0–5.0) */
    private double averageRating;
    private long totalRatings;

    /** From effectiveness tracking */
    private double stabilizationRate;
    private double declineRate;
    private double appointmentShowRate;

    /** Current number of patients assigned */
    private int currentPatientCount;

    /** Whether the doctor has risk flags */
    private boolean hasRiskFlags;
}
