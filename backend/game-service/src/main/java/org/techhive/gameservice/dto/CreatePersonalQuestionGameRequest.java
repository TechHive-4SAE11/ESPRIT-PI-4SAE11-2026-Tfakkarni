package org.techhive.gameservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreatePersonalQuestionGameRequest {

  @NotBlank(message = "Title is required")
  @Size(max = 20, message = "Title must be at most 20 characters")
  @Pattern(regexp = "^[a-zA-Z0-9 ]+$", message = "Title can only contain letters, numbers, and spaces")
  private String title;

  @Size(max = 100, message = "Description must be at most 100 characters")
  private String description;

  @NotEmpty(message = "At least one question is required")
  @Valid
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

    @NotBlank(message = "Question text is required")
    @Size(max = 500, message = "Question text must be at most 500 characters")
    private String questionText;

    @NotBlank(message = "Correct answer is required")
    @Size(max = 500, message = "Correct answer must be at most 500 characters")
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
