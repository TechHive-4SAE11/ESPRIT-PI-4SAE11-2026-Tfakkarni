package org.techhive.gameservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "custom_game_attempts")
public class CustomGameAttempt {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "custom_game_id")
  private CustomGame customGame;

  @Column(name = "player_keycloak_id", nullable = false)
  private String playerKeycloakId;

  @Column(nullable = false)
  private int score;

  @Column(name = "total_questions", nullable = false)
  private int totalQuestions;

  @Column(name = "duration_seconds")
  private Integer durationSeconds;

  @Column(name = "completed_at", nullable = false)
  private LocalDateTime completedAt;

  public CustomGameAttempt() {
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

  public CustomGame getCustomGame() {
    return customGame;
  }

  public void setCustomGame(CustomGame customGame) {
    this.customGame = customGame;
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

  public Integer getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(Integer durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }
}
