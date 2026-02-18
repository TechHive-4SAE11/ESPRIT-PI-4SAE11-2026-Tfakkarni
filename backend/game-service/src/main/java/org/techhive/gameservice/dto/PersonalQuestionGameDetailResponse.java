package org.techhive.gameservice.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full game detail response including all questions with answers, used for edit
 * view.
 */
public class PersonalQuestionGameDetailResponse {
  private Long id;
  private String patientKeycloakId;
  private String title;
  private String description;
  private List<QuestionItemDetail> questions;
  private LocalDateTime createdAt;

  public PersonalQuestionGameDetailResponse() {
  }

  public PersonalQuestionGameDetailResponse(Long id, String patientKeycloakId, String title, String description,
      List<QuestionItemDetail> questions, LocalDateTime createdAt) {
    this.id = id;
    this.patientKeycloakId = patientKeycloakId;
    this.title = title;
    this.description = description;
    this.questions = questions;
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

  public List<QuestionItemDetail> getQuestions() {
    return questions;
  }

  public void setQuestions(List<QuestionItemDetail> questions) {
    this.questions = questions;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public static class QuestionItemDetail {
    private Long id;
    private String questionText;
    private String correctAnswer;

    public QuestionItemDetail() {
    }

    public QuestionItemDetail(Long id, String questionText, String correctAnswer) {
      this.id = id;
      this.questionText = questionText;
      this.correctAnswer = correctAnswer;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getQuestionText() {
      return questionText;
    }

    public void setQuestionText(String questionText) {
      this.questionText = questionText;
    }

    public String getCorrectAnswer() {
      return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
      this.correctAnswer = correctAnswer;
    }
  }
}
