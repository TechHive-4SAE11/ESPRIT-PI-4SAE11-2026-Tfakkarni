package org.techhive.gameservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MovieGameAttemptResponse {
  private Long attemptId;
  private int score;
  private int totalQuestions;
  private int durationSeconds;
  private double percentage;
  private List<MovieAnswerResult> results;
  private LocalDateTime completedAt;

  public MovieGameAttemptResponse() {
  }

  public Long getAttemptId() {
    return attemptId;
  }

  public void setAttemptId(Long attemptId) {
    this.attemptId = attemptId;
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

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(int durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public double getPercentage() {
    return percentage;
  }

  public void setPercentage(double percentage) {
    this.percentage = percentage;
  }

  public List<MovieAnswerResult> getResults() {
    return results;
  }

  public void setResults(List<MovieAnswerResult> results) {
    this.results = results;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public static class MovieAnswerResult {
    private Long itemId;
    private String posterUrl;
    private String movieTitle;
    private String correctAnswer;
    private String selectedAnswer;
    private boolean correct;

    public MovieAnswerResult() {
    }

    public MovieAnswerResult(Long itemId, String posterUrl, String movieTitle, String correctAnswer,
        String selectedAnswer,
        boolean correct) {
      this.itemId = itemId;
      this.posterUrl = posterUrl;
      this.movieTitle = movieTitle;
      this.correctAnswer = correctAnswer;
      this.selectedAnswer = selectedAnswer;
      this.correct = correct;
    }

    public Long getItemId() {
      return itemId;
    }

    public void setItemId(Long itemId) {
      this.itemId = itemId;
    }

    public String getPosterUrl() {
      return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
      this.posterUrl = posterUrl;
    }

    public String getMovieTitle() {
      return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
      this.movieTitle = movieTitle;
    }

    public String getCorrectAnswer() {
      return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
      this.correctAnswer = correctAnswer;
    }

    public String getSelectedAnswer() {
      return selectedAnswer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
      this.selectedAnswer = selectedAnswer;
    }

    public boolean isCorrect() {
      return correct;
    }

    public void setCorrect(boolean correct) {
      this.correct = correct;
    }
  }
}
