package org.techhive.gameservice.dto;

public class GameStatsResponse {
    private String playerKeycloakId;
    private String playerName;
    private int totalGamesCreated;
    private int totalGamesPlayed;
    private double averageScore;
    private int bestScore;
    private int totalAttempts;

    public GameStatsResponse() {
    }

    public String getPlayerKeycloakId() { return playerKeycloakId; }
    public void setPlayerKeycloakId(String playerKeycloakId) { this.playerKeycloakId = playerKeycloakId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getTotalGamesCreated() { return totalGamesCreated; }
    public void setTotalGamesCreated(int totalGamesCreated) { this.totalGamesCreated = totalGamesCreated; }
    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(int totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }
    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
    public int getBestScore() { return bestScore; }
    public void setBestScore(int bestScore) { this.bestScore = bestScore; }
    public int getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }
}
