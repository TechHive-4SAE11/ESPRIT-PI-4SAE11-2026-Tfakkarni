package org.techhive.medicalservice.entity.coaching;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coaching_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingProgress implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coaching_goal_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CoachingGoal coachingGoal;

    @Column(name = "date_recorded", nullable = false)
    private LocalDate dateRecorded;

    @Column(name = "completion_percentage")
    private Integer completionPercentage;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CoachingMood mood;

    @Column(name = "energy_level")
    private Integer energyLevel;

    @Column(name = "helper_notes", columnDefinition = "TEXT")
    private String helperNotes;

    @Column(name = "patient_feedback", columnDefinition = "TEXT")
    private String patientFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "recorded_by_role", nullable = false, length = 20)
    private ProgressRecordedByRole recordedByRole;

    @Column(name = "recorded_by_user_id", length = 255)
    private String recordedByUserId;

    @Column(name = "weather_summary", length = 500)
    private String weatherSummary;

    @Column(name = "weather_fetched_at")
    private LocalDateTime weatherFetchedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
