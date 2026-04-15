package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_ratings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long meetingId;

    @Column(nullable = false)
    private String doctorKeycloakId;

    @Column(nullable = false)
    private String patientKeycloakId;

    /** 1–5 stars */
    @Column(nullable = false)
    private Integer rating;

    /** Mandatory when rating <= 3 */
    @Column(columnDefinition = "TEXT")
    private String review;

    private String doctorName;
    private String patientName;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
