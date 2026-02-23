package org.techhive.gameservice.dto;

import java.util.List;

/**
 * Play data for personal questions game.
 * Includes the correct answer for each question because scoring is
 * self-assessed
 * (the patient decides if their typed answer is close enough).
 */
public class PersonalQuestionPlayData {
  private Long gameId;
  private String title;
  private String description;
  private List<PersonalQuestion> questions;
  private int totalQuestions;

  public PersonalQuestionPlayData() {
  }

  public Long getGameId() {
    return gameId;
  }

  public void setGameId(Long gameId) {
    this.gameId = gameId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<PersonalQuestion> getQuestions() {
    return questions;
  }

  public void setQuestions(List<PersonalQuestion> questions) {
    this.questions = questions;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public void setTotalQuestions(int totalQuestions) {
    this.totalQuestions = totalQuestions;
  }

  public static class PersonalQuestion {
    private Long itemId;
    private String questionText;
    private String correctAnswer;

    public PersonalQuestion() {
    }

    public Long getItemId() {
      return itemId;
    }

    public void setItemId(Long itemId) {
      this.itemId = itemId;
    }

    public String getQuestionText() {
      return questionText;
    }

    public void setQuestionText(String questionText) {
      this.questionText = questionText;
    }

    public String getCorrectAnswer() {
      return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
      this.correctAnswer = correctAnswer;
    }
  }
}
