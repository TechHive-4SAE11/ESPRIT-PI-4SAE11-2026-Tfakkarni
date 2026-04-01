package org.techhive.alertservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "safe_zones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafeZone {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String patientId;

  @Column(nullable = false)
  private String name;

  /** JSON array of {lat, lng} coordinate pairs defining the polygon */
  @Column(columnDefinition = "TEXT", nullable = false)
  private String points;

  @Column(nullable = false)
  private boolean active = true;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
