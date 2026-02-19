package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "medication_intake_logs")
@Data @NoArgsConstructor @AllArgsConstructor
public class MedicationIntakeLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;

    @Column(name = "taken_at")
    private String takenAt;

    @Column(name = "status")
    private String status;  // PRIS, OUBLIE, REFUSE, EN_RETARD

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
