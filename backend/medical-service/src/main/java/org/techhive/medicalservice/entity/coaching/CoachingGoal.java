package org.techhive.medicalservice.entity.coaching;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "coaching_goals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingGoal implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_folder_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MedicalFolder medicalFolder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Diagnostics diagnostics;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 40)
    private CoachingGoalType goalType;

    @Column(name = "goal_title", nullable = false, length = 500)
    private String goalTitle;

    @Column(name = "action_steps", columnDefinition = "TEXT")
    private String actionSteps;

    /** JSON array or plain text tips */
    @Column(name = "tips", columnDefinition = "TEXT")
    private String tips;

    @Column(name = "target_days")
    private Integer targetDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CoachingGoalStatus status = CoachingGoalStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private CoachingPriority priority = CoachingPriority.MEDIUM;

    @Column(name = "outdoor_activity")
    @Builder.Default
    private boolean outdoorActivity = false;

    private Double latitude;
    private Double longitude;

    @Column(name = "created_by_doctor_id", length = 255)
    private String createdByDoctorId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Throttle stale reminders */
    @Column(name = "last_stale_notification_at")
    private LocalDateTime lastStaleNotificationAt;
}
