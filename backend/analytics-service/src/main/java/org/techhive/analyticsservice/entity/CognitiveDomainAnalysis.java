package org.techhive.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cognitive_domain_analyses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CognitiveDomainAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_keycloak_id", nullable = false)
    private String patientKeycloakId;

    @Column(name = "domain_name", nullable = false)
    private String domainName;

    @Column(name = "correct_count")
    private int correctCount;

    @Column(name = "incorrect_count")
    private int incorrectCount;

    @Column(name = "accuracy_pct")
    private Double accuracyPct;

    @Column(name = "trend")
    @Enumerated(EnumType.STRING)
    private ScoreTrend trend;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.computedAt = LocalDateTime.now();
    }
}
