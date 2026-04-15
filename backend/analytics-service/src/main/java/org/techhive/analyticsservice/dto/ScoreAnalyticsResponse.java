package org.techhive.analyticsservice.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreAnalyticsResponse {
    private String patientKeycloakId;
    private int totalGamesPlayed;
    private int gamesLast7Days;
    private double averageScore;
    private double averageScoreLast7Days;
    private int bestScore;
    private List<AttemptPoint> scoreHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttemptPoint {
        private Long attemptId;
        private String gameType;
        private String gameTitle;
        private int score;
        private int totalQuestions;
        private double percentage;
        private Integer durationSeconds;
        private LocalDateTime completedAt;
    }
}
