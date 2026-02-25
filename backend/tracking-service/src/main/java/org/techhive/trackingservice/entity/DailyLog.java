package org.techhive.trackingservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "daily_logs")
@Data @NoArgsConstructor @AllArgsConstructor
public class DailyLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_keycloak_id", nullable = false)
    private String patientKeycloakId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "global_notes", columnDefinition = "TEXT")
    private String globalNotes;

    /** Humeur du jour : BONNE, MOYENNE, MAUVAISE (nullable si non renseigné) */
    @Column(name = "mood_level")
    private String moodLevel;

    /** Durée du sommeil en heures (nullable si non renseigné) */
    @Column(name = "sleep_hours")
    private Double sleepHours;

    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NutritionEntry> nutritionEntries = new ArrayList<>();

    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicationIntakeLog> medicationIntakes = new ArrayList<>();

    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityEntry> activityEntries = new ArrayList<>();

    @OneToMany(mappedBy = "dailyLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IncidentEntry> incidentEntries = new ArrayList<>();

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
