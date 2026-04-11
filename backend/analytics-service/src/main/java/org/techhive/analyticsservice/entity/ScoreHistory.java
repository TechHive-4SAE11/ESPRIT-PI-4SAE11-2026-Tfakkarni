package org.techhive.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "score_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_keycloak_id", nullable = false)
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
    private AlzheimerStage stage;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = LocalDateTime.now();
    }
}
