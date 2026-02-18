package org.techhive.gameservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memory_tags")
public class MemoryTag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "patient_keycloak_id", nullable = false)
  private String patientKeycloakId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String color;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public MemoryTag() {
  }

  public MemoryTag(String patientKeycloakId, String name, String color) {
    this.patientKeycloakId = patientKeycloakId;
    this.name = name;
    this.color = color;
  }

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPatientKeycloakId() {
    return patientKeycloakId;
  }

  public void setPatientKeycloakId(String patientKeycloakId) {
    this.patientKeycloakId = patientKeycloakId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
