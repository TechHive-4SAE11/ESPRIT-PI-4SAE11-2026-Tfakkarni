package org.techhive.gameservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "photo_memories")
public class PhotoMemory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "patient_keycloak_id", nullable = false)
  private String patientKeycloakId;

  @Column(nullable = false)
  private String name;

  @Column(name = "image_data", nullable = false, columnDefinition = "BYTEA")
  private byte[] imageData;

  @Column(name = "image_content_type", nullable = false)
  private String imageContentType;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "photo_memory_tags", joinColumns = @JoinColumn(name = "photo_memory_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<MemoryTag> tags = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public PhotoMemory() {
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

  public byte[] getImageData() {
    return imageData;
  }

  public void setImageData(byte[] imageData) {
    this.imageData = imageData;
  }

  public String getImageContentType() {
    return imageContentType;
  }

  public void setImageContentType(String imageContentType) {
    this.imageContentType = imageContentType;
  }

  public Set<MemoryTag> getTags() {
    return tags;
  }

  public void setTags(Set<MemoryTag> tags) {
    this.tags = tags;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
