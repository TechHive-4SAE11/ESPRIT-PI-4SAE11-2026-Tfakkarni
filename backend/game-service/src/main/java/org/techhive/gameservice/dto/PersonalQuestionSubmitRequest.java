package org.techhive.gameservice.dto;

/**
 * Submit request for personal questions game.
 * The score is self-assessed by the patient (they decide if each answer was
 * correct).
 */
public class PersonalQuestionSubmitRequest {
  private int score;
  private int totalQuestions;
  private int durationSeconds;

  public PersonalQuestionSubmitRequest() {
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
}
