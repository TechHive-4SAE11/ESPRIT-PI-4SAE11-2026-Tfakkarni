package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "care_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CareActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "care_plan_id", nullable = false)
    private CarePlan carePlan;

    @Column(name = "activity_name", nullable = false)
    private String activityName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "frequency")
    private String frequency;

    @Column(name = "duration")
    private String duration;

    @Column(name = "completion_status")
    private String completionStatus; // e.g., "PENDING", "COMPLETED"

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
