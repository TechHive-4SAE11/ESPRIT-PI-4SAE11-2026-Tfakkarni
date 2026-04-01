package org.techhive.alertservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "geofence_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceAlert {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String patientId;

  @Column(nullable = false)
  private Double latitude;

  @Column(nullable = false)
  private Double longitude;

  private String safeZoneName;

  @Column(nullable = false)
  private boolean acknowledged = false;

  private LocalDateTime acknowledgedAt;

  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
