package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.techhive.gameservice.entity.Answer;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // Find all answers for a question
    List<Answer> findByQuestionId(Long questionId);

    // Find correct answer for a question
    Optional<Answer> findByQuestionIdAndIsCorrectTrue(Long questionId);

    // Delete all answers for a question (derived)
    void deleteByQuestionId(Long questionId);

    // Count answers for a question
    long countByQuestionId(Long questionId);

    // Search answers by text
    List<Answer> findByTextContainingIgnoreCase(String keyword);

    // Check if answer is correct
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Answer a WHERE a.id = :answerId AND a.isCorrect = true")
    boolean isCorrectAnswer(@Param("answerId") Long answerId);
}
