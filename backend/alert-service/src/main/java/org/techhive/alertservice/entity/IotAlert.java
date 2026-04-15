package org.techhive.alertservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "iot_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String alertType; // ELEVATED_BPM, LOW_BPM

    @Column(nullable = false)
    private int value; // the BPM reading

    private String message;

    @Column(nullable = false)
    private boolean acknowledged = false;

    private LocalDateTime acknowledgedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
