package org.techhive.iotservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "heartbeat_readings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeartbeatReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "bpm", nullable = false)
    private int bpm;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
