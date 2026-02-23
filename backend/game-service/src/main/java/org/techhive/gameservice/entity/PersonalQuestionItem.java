package org.techhive.gameservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "personal_question_items")
public class PersonalQuestionItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id", nullable = false)
  private PersonalQuestionGame game;

  @Column(name = "question_text", nullable = false, length = 500)
  private String questionText;

  @Column(name = "correct_answer", nullable = false, length = 500)
  private String correctAnswer;

  @Column(name = "display_order")
  private int displayOrder;

  public PersonalQuestionItem() {
  }

  public PersonalQuestionItem(PersonalQuestionGame game, String questionText, String correctAnswer, int displayOrder) {
    this.game = game;
    this.questionText = questionText;
    this.correctAnswer = correctAnswer;
    this.displayOrder = displayOrder;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public PersonalQuestionGame getGame() {
    return game;
  }

  public void setGame(PersonalQuestionGame game) {
    this.game = game;
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

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}
