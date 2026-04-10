package org.techhive.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_effectiveness_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorEffectivenessScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_keycloak_id", nullable = false, unique = true)
    private String doctorKeycloakId;

    @Column(name = "patient_count")
    private int patientCount;

    @Column(name = "stabilization_rate")
    private Double stabilizationRate;

    @Column(name = "decline_rate")
    private Double declineRate;

    @Column(name = "avg_compliance_improvement")
    private Double avgComplianceImprovement;

    @Column(name = "session_frequency")
    private Double sessionFrequency;

    @Column(name = "coaching_completion_rate")
    private Double coachingCompletionRate;

    @Column(name = "appointment_show_rate")
    private Double appointmentShowRate;

    @Column(name = "risk_flags", columnDefinition = "TEXT")
    private String riskFlags;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.computedAt = LocalDateTime.now();
    }
}
