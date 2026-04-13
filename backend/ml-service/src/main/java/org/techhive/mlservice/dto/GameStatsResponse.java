package org.techhive.mlservice.dto;

import lombok.Data;

@Data
public class GameStatsResponse {
    private int totalGamesPlayed;
    private double averageScore;
    private int bestScore;
    private int photoCount;
    private int placeCount;
    private int movieCount;
    private int questionCount;
}