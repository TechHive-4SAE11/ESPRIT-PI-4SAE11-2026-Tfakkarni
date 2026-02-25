package org.techhive.gameservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Analytics response for the doctor view — contains score history and summary
 * stats.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreAnalyticsResponse {

  private String patientKeycloakId;

  // Summary
  private int totalGamesPlayed;
  private int gamesLast7Days;
  private double averageScore;
  private double averageScoreLast7Days;
  private int bestScore;

  // Score history for charting (all attempts ordered by date)
  private List<AttemptPoint> scoreHistory;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AttemptPoint {
    private Long attemptId;
    private String gameType; // "CUSTOM", "MINI", "MOVIE", "PERSONAL"
    private String gameTitle;
    private int score;
    private int totalQuestions;
    private double percentage;
    private Integer durationSeconds;
    private LocalDateTime completedAt;
  }
}
