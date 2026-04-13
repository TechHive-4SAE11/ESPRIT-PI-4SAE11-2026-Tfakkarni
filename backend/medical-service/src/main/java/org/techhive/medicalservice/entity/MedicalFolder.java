package org.techhive.medicalservice.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "medical_folders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalFolder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Patient ID cannot be null")
    @NotBlank(message = "Patient ID cannot be blank")
    @Size(min = 1, max = 255, message = "Patient ID must be between 1 and 255 characters")
    @Column(name = "id_patient", nullable = false)
    private String patientId;

    @NotNull(message = "Doctor ID cannot be null")
    @NotBlank(message = "Doctor ID cannot be blank")
    @Size(min = 1, max = 255, message = "Doctor ID must be between 1 and 255 characters")
    @Column(name = "id_doctor", nullable = false)
    private String doctorId;

    @Size(max = 10, message = "Blood type must not exceed 10 characters")
    @Column(name = "blood_type", length = 10)
    private String bloodType;

    @Column(name = "height")
    private Double height;

    @Column(name = "weight")
    private Double weight;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // --- Patient attendance / no-show monitoring (updated by
    // AttendanceMonitoringService) ---

    @Builder.Default
    @Column(name = "consecutive_no_shows", nullable = false)
    @ColumnDefault("0")
    private int consecutiveNoShows = 0;

    @Builder.Default
    @Column(name = "total_no_shows", nullable = false)
    @ColumnDefault("0")
    private int totalNoShows = 0;

    @Builder.Default
    @Column(name = "booking_restricted", nullable = false)
    @ColumnDefault("false")
    private boolean bookingRestricted = false;

    @Column(name = "restriction_reason", length = 500)
    private String restrictionReason;

    @Builder.Default
    @Column(name = "manual_review_required", nullable = false)
    @ColumnDefault("false")
    private boolean manualReviewRequired = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "attendance_risk_level", length = 20, nullable = false)
    @ColumnDefault("'NONE'")
    private AttendanceRiskLevel attendanceRiskLevel = AttendanceRiskLevel.NONE;

    /**
     * When true, automatic restriction is not applied while consecutive no-shows
     * remain high
     * (cleared by staff after review). Reset when streak drops below 3.
     */
    @Builder.Default
    @Column(name = "attendance_restriction_overridden", nullable = false)
    @ColumnDefault("false")
    private boolean attendanceRestrictionOverridden = false;

    /** Inverse relationship with AIReport for cascade delete */
    @OneToMany(mappedBy = "medicalFolder", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<AIReport> aiReports;

    /** Inverse relationship with Diagnostics for cascade delete */
    @OneToMany(mappedBy = "medicalFolder", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Diagnostics> diagnostics;

    /** Inverse relationship with MedicalHistory for cascade delete */
    @OneToMany(mappedBy = "medicalFolder", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<MedicalHistory> medicalHistories;
}