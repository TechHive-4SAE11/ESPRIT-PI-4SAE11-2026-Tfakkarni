package org.techhive.gameservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memory_places")
public class MemoryPlace {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Double latitude;

  @Column(nullable = false)
  private Double longitude;

  private String hint;

  @Column(name = "patient_keycloak_id", nullable = false)
  private String patientKeycloakId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public MemoryPlace() {
  }

  public MemoryPlace(String name, Double latitude, Double longitude, String hint, String patientKeycloakId) {
    this.name = name;
    this.latitude = latitude;
    this.longitude = longitude;
    this.hint = hint;
    this.patientKeycloakId = patientKeycloakId;
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  // ─── Getters & Setters ─────────────────────────────────────

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public String getHint() {
    return hint;
  }

  public void setHint(String hint) {
    this.hint = hint;
  }

  public String getPatientKeycloakId() {
    return patientKeycloakId;
  }

  public void setPatientKeycloakId(String patientKeycloakId) {
    this.patientKeycloakId = patientKeycloakId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
