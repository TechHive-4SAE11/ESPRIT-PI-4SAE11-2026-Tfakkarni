package org.techhive.gameservice.dto;

import java.util.List;

public class MovieGameSubmitRequest {
  private List<MovieAnswerEntry> answers;
  private int durationSeconds;

  public MovieGameSubmitRequest() {
  }

  public List<MovieAnswerEntry> getAnswers() {
    return answers;
  }

  public void setAnswers(List<MovieAnswerEntry> answers) {
    this.answers = answers;
  }

  public int getDurationSeconds() {
    return durationSeconds;
  }

  public void setDurationSeconds(int durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public static class MovieAnswerEntry {
    private Long itemId;
    private String selectedAnswer;

    public MovieAnswerEntry() {
    }

    public Long getItemId() {
      return itemId;
    }

    public void setItemId(Long itemId) {
      this.itemId = itemId;
    }

    public String getSelectedAnswer() {
      return selectedAnswer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
      this.selectedAnswer = selectedAnswer;
    }
  }
}
