package org.techhive.gameservice.dto;

public class OverviewStatsResponse {
    private long totalGames;
    private long totalAttempts;
    private long totalPlayers;
    private double averageScorePercentage;

    public OverviewStatsResponse() {
    }

    public long getTotalGames() { return totalGames; }
    public void setTotalGames(long totalGames) { this.totalGames = totalGames; }
    public long getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(long totalAttempts) { this.totalAttempts = totalAttempts; }
    public long getTotalPlayers() { return totalPlayers; }
    public void setTotalPlayers(long totalPlayers) { this.totalPlayers = totalPlayers; }
    public double getAverageScorePercentage() { return averageScorePercentage; }
    public void setAverageScorePercentage(double averageScorePercentage) { this.averageScorePercentage = averageScorePercentage; }
}
