package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_meetings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomName;

    private String roomUrl;

    private String dailyRoomId;

    @Column(nullable = false)
    private String doctorKeycloakId;

    @Column(nullable = false)
    private String patientKeycloakId;

    // The helper/caregiver who actually joins on behalf of the patient
    private String helperKeycloakId;

    private String patientName;

    private String doctorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    /** Full live transcript captured during the meeting */
    @Column(columnDefinition = "TEXT")
    private String transcript;

    /** JSON array of periodic AI mini-summaries (every N minutes) */
    @Column(columnDefinition = "TEXT")
    private String transcriptSummaries;

    private LocalDateTime scheduledAt;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime createdAt;

    private Integer durationMinutes;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
