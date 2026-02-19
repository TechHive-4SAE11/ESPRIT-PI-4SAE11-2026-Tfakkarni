package org.techhive.gameservice.service;

import org.techhive.gameservice.dto.QuizDTO;
import org.techhive.gameservice.entity.Quiz;

import java.time.LocalDateTime;
import java.util.List;

public interface IQuizService {

    // Basic CRUD
    Quiz createQuiz(QuizDTO quizDTO);
    Quiz getQuizById(long id);
    Quiz updateQuiz(QuizDTO quizDTO);
    void deleteQuiz(long id);
    List<Quiz> getAllQuizzes();

    // Additional methods
    List<Quiz> getQuizzesByCaregiverId(Long caregiverId);
    List<Quiz> searchQuizzesByTopic(String topic);
    List<Quiz> getRecentQuizzesByCaregiver(Long caregiverId, int limit);
    List<Quiz> getQuizzesByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    List<Quiz> getQuizzesWithMinScore(Integer minScore);
    long getQuizCountByCaregiver(Long caregiverId);

    // Business methods
    Quiz startQuiz(Long quizId);
    Quiz completeQuiz(Long quizId, Integer score);
    double getAverageScoreByCaregiver(Long caregiverId);
    List<String> getWeakTopicsByCaregiver(Long caregiverId);
}
