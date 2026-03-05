package org.techhive.userservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import java.util.List;

@Entity
@Table(name = "users")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "keycloak_id", unique = true, nullable = false)
  private String keycloakId;

  @Column(nullable = false)
  private String firstName;

  @Column(nullable = false)
  private String lastName;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(nullable = false)
  private String role;

  @Column
  private String gender;

  @Column(nullable = false, columnDefinition = "boolean default true")
  private boolean enabled = true;

  @Column(name = "kyc_status", nullable = false, columnDefinition = "varchar(255) default 'none'")
  private String kycStatus = "none";

  @Column(name = "kyc_session_id")
  private String kycSessionId;

  @Column(name = "signature_image", columnDefinition = "BYTEA")
  private byte[] signatureImage;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public User() {
  }

  public User(String keycloakId, String firstName, String lastName, String email, String role) {
    this.keycloakId = keycloakId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.role = role;
    this.enabled = true;
  }

  public User(String keycloakId, String firstName, String lastName, String email, String role, String gender) {
    this.keycloakId = keycloakId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.role = role;
    this.gender = gender;
    this.enabled = true;
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

  public String getKeycloakId() {
    return keycloakId;
  }

  public void setKeycloakId(String keycloakId) {
    this.keycloakId = keycloakId;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getGender() {
    return gender;
  }

  public void setGender(String gender) {
    this.gender = gender;
  }

  public String getKycStatus() {
    return kycStatus;
  }

  public void setKycStatus(String kycStatus) {
    this.kycStatus = kycStatus;
  }

  public String getKycSessionId() {
    return kycSessionId;
  }

  public void setKycSessionId(String kycSessionId) {
    this.kycSessionId = kycSessionId;
  }

  public byte[] getSignatureImage() {
    return signatureImage;
  }

  public void setSignatureImage(byte[] signatureImage) {
    this.signatureImage = signatureImage;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
