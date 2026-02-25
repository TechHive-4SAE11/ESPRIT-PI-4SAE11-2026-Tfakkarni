package org.techhive.gameservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "personal_question_attempts")
public class PersonalQuestionAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id", nullable = false)
  private PersonalQuestionGame game;

  @Column(name = "player_keycloak_id", nullable = false)
  private String playerKeycloakId;

  @Column(nullable = false)
  private int score;

  @Column(name = "total_questions", nullable = false)
  private int totalQuestions;

  @Column(name = "completed_at", nullable = false)
  private LocalDateTime completedAt;

  @Column(name = "duration_seconds")
  private int durationSeconds;

  public PersonalQuestionAttempt() {
  }

  public PersonalQuestionAttempt(PersonalQuestionGame game, String playerKeycloakId, int score,
      int totalQuestions, int durationSeconds) {
    this.game = game;
    this.playerKeycloakId = playerKeycloakId;
    this.score = score;
    this.totalQuestions = totalQuestions;
    this.durationSeconds = durationSeconds;
    this.completedAt = LocalDateTime.now();
  }

  @PrePersist
  protected void onCreate() {
    if (this.completedAt == null) {
      this.completedAt = LocalDateTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public PersonalQuestionGame getGame() {
    return game;
  }

  public void setGame(PersonalQuestionGame game) {
    this.game = game;
  }

  public String getPlayerKeycloakId() {
    return playerKeycloakId;
  }

  public void setPlayerKeycloakId(String playerKeycloakId) {
    this.playerKeycloakId = playerKeycloakId;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public void setTotalQuestions(int totalQuestions) {
    this.totalQuestions = totalQuestions;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(int durationSeconds) {
    this.durationSeconds = durationSeconds;
  }
}
