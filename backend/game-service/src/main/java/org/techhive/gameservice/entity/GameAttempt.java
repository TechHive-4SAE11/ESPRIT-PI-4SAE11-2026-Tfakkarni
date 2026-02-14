package org.techhive.gameservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "game_attempts")
public class GameAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mini_game_id", nullable = false)
    private MiniGame miniGame;

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

    public GameAttempt() {
    }

    public GameAttempt(MiniGame miniGame, String playerKeycloakId, int score, int totalQuestions, int durationSeconds) {
        this.miniGame = miniGame;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MiniGame getMiniGame() { return miniGame; }
    public void setMiniGame(MiniGame miniGame) { this.miniGame = miniGame; }
    public String getPlayerKeycloakId() { return playerKeycloakId; }
    public void setPlayerKeycloakId(String playerKeycloakId) { this.playerKeycloakId = playerKeycloakId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
}
