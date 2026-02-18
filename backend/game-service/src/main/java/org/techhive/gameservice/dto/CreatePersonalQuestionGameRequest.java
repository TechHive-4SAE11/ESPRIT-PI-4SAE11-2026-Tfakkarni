package org.techhive.gameservice.dto;

import java.util.List;

public class CreatePersonalQuestionGameRequest {
  private String title;
  private String description;
  private List<QuestionItemRequest> questions;

  public CreatePersonalQuestionGameRequest() {
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

  public List<QuestionItemRequest> getQuestions() {
    return questions;
  }

  public void setQuestions(List<QuestionItemRequest> questions) {
    this.questions = questions;
  }

  public static class QuestionItemRequest {
    private String questionText;
    private String correctAnswer;

    public QuestionItemRequest() {
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
