package org.techhive.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStatsResponse {
    private int totalGamesPlayed;
    private double averageScore;
    private int totalAttempts;
    private int bestScore;
}
