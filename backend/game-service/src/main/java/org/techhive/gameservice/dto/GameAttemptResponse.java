package org.techhive.gameservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GameAttemptResponse {
    private Long attemptId;
    private int score;
    private int totalQuestions;
    private int durationSeconds;
    private double percentage;
    private List<AnswerResult> results;
    private LocalDateTime completedAt;

    public GameAttemptResponse() {
    }

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public List<AnswerResult> getResults() { return results; }
    public void setResults(List<AnswerResult> results) { this.results = results; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public static class AnswerResult {
        private Long imageId;
        private String correctName;
        private String selectedName;
        private boolean correct;

        public AnswerResult() {
        }

        public AnswerResult(Long imageId, String correctName, String selectedName, boolean correct) {
            this.imageId = imageId;
            this.correctName = correctName;
            this.selectedName = selectedName;
            this.correct = correct;
        }

        public Long getImageId() { return imageId; }
        public void setImageId(Long imageId) { this.imageId = imageId; }
        public String getCorrectName() { return correctName; }
        public void setCorrectName(String correctName) { this.correctName = correctName; }
        public String getSelectedName() { return selectedName; }
        public void setSelectedName(String selectedName) { this.selectedName = selectedName; }
        public boolean isCorrect() { return correct; }
        public void setCorrect(boolean correct) { this.correct = correct; }
    }
}
