package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_keycloak_id", nullable = false)
    private String doctorKeycloakId;

    @Column(name = "patient_keycloak_id", nullable = false)
    private String patientKeycloakId;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "incident_type")
    private String incidentType;

    @Column(name = "severity", nullable = false)
    private String severity; // MODERE ou GRAVE

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "location")
    private String location;

    @Column(name = "action_taken")
    private String actionTaken;

    @Column(name = "occurred_at")
    private String occurredAt;

    @Column(name = "log_date")
    private String logDate;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
