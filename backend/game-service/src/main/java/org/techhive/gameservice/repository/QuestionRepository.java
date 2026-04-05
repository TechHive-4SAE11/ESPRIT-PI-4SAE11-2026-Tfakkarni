package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.techhive.gameservice.entity.Question;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Find all questions for a quiz
    List<Question> findByQuizId(Long quizId);

    // Find questions by difficulty level
    List<Question> findByDifficultyLevel(Integer difficultyLevel);

    // Delete all questions for a quiz (derived)
    void deleteByQuizId(Long quizId);

    // Count questions for a quiz
    long countByQuizId(Long quizId);

    // Search questions by text
    List<Question> findByTextContainingIgnoreCase(String keyword);

    // Find questions by difficulty level and quiz
    List<Question> findByQuizIdAndDifficultyLevel(Long quizId, Integer difficultyLevel);
}
