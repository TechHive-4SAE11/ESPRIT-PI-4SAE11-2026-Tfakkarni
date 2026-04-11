package org.techhive.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_gates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureGate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_keycloak_id", nullable = false, unique = true)
    private String patientKeycloakId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlzheimerStage stage;

    @Column(name = "iot_enabled")
    private boolean iotEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "iot_level")
    private IotLevel iotLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_complexity")
    private GameComplexity gameComplexity;

    @Enumerated(EnumType.STRING)
    @Column(name = "monitoring_level")
    private MonitoringLevel monitoringLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_escalation")
    private EscalationLevel notificationEscalation;

    @Enumerated(EnumType.STRING)
    @Column(name = "ui_mode")
    private UiMode uiMode;

    @Column(name = "safe_zone_required")
    private boolean safeZoneRequired;

    @Column(name = "meeting_suggested_frequency_days")
    private int meetingSuggestedFrequencyDays;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.computedAt = LocalDateTime.now();
    }
}
