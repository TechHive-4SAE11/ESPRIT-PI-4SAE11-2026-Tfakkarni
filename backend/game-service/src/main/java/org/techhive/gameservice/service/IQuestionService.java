package org.techhive.gameservice.service;

import org.techhive.gameservice.dto.QuestionDTO;
import org.techhive.gameservice.entity.Question;

import java.util.List;

public interface IQuestionService {

    // Basic CRUD
    Question createQuestion(QuestionDTO questionDTO);
    Question getQuestionById(long id);
    Question updateQuestion(QuestionDTO questionDTO);
    void deleteQuestion(long id);
    List<Question> getAllQuestions();

    // Additional methods
    List<Question> getQuestionsByQuizId(Long quizId);
    List<Question> getQuestionsByDifficultyLevel(Integer difficultyLevel);
    void deleteQuestionsByQuizId(Long quizId);
    long getQuestionCountByQuizId(Long quizId);
    List<Question> searchQuestions(String keyword);
    List<Question> getQuestionsByQuizAndDifficulty(Long quizId, Integer difficultyLevel);
    boolean validateQuestion(QuestionDTO questionDTO);
    int calculateTotalPoints(Long quizId);
}
