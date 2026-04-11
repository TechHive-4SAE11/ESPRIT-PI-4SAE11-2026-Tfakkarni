package org.techhive.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_composite_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientCompositeScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_keycloak_id", nullable = false, unique = true)
    private String patientKeycloakId;

    @Column(name = "cognitive_score")
    private Double cognitiveScore;

    @Column(name = "daily_functioning_score")
    private Double dailyFunctioningScore;

    @Column(name = "medical_stability_score")
    private Double medicalStabilityScore;

    @Column(name = "iot_risk_score")
    private Double iotRiskScore;

    @Column(name = "engagement_score")
    private Double engagementScore;

    @Column(name = "overall_score")
    private Double overallScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlzheimerStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "score_trend")
    private ScoreTrend scoreTrend;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.computedAt = LocalDateTime.now();
    }
}
