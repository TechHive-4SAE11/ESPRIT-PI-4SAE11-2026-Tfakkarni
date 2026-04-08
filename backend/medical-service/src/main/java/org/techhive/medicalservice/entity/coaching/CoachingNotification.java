package org.techhive.medicalservice.entity.coaching;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.techhive.medicalservice.entity.MedicalFolder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "coaching_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachingNotification implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medical_folder_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MedicalFolder medicalFolder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coaching_goal_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CoachingGoal coachingGoal;

    @Column(name = "recipient_user_id", nullable = false, length = 255)
    private String recipientUserId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
