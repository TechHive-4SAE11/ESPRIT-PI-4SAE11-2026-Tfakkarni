package org.techhive.gameservice.dto;

import java.time.LocalDateTime;

public class PersonalQuestionGameResponse {
  private Long id;
  private String patientKeycloakId;
  private String title;
  private String description;
  private int questionCount;
  private LocalDateTime createdAt;

  public PersonalQuestionGameResponse() {
  }

  public PersonalQuestionGameResponse(Long id, String patientKeycloakId, String title, String description,
      int questionCount, LocalDateTime createdAt) {
    this.id = id;
    this.patientKeycloakId = patientKeycloakId;
    this.title = title;
    this.description = description;
    this.questionCount = questionCount;
    this.createdAt = createdAt;
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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getQuestionCount() {
    return questionCount;
  }

  public void setQuestionCount(int questionCount) {
    this.questionCount = questionCount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
