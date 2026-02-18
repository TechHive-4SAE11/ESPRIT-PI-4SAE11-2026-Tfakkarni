package org.techhive.gameservice.dto;

import java.util.List;

public class EditPersonalQuestionGameRequest {
  private String title;
  private String description;
  private List<QuestionItemEntry> questions;

  public EditPersonalQuestionGameRequest() {
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

  public List<QuestionItemEntry> getQuestions() {
    return questions;
  }

  public void setQuestions(List<QuestionItemEntry> questions) {
    this.questions = questions;
  }

  public static class QuestionItemEntry {
    private Long id; // null for new items
    private String questionText;
    private String correctAnswer;

    public QuestionItemEntry() {
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
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
