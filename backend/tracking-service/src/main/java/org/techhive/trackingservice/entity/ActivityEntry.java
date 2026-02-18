package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "activity_entries")
@Data @NoArgsConstructor @AllArgsConstructor
public class ActivityEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_log_id", nullable = false)
    private DailyLog dailyLog;

    private String activityType; // PHYSIQUE, COGNITIVE, SOCIALE, HYGIENE, PROMENADE, AUTRE
    private String description;
    private Integer durationMinutes;
    private String intensity;    // FAIBLE, MODERE, ELEVE
    private String notes;
    private String startTime;
}