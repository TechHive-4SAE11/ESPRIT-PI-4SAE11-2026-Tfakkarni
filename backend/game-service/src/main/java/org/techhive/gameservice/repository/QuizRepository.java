package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.*;

import org.springframework.data.repository.query.Param;
import org.techhive.gameservice.entity.Quiz;

import java.time.LocalDateTime;
import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    // Find quizzes by caregiver
    List<Quiz> findByCaregiverId(Long caregiverId);

    // Find quizzes by topic
    List<Quiz> findByTopicContainingIgnoreCase(String topic);

    // Find quizzes taken after a certain date
    List<Quiz> findByDateTakenAfter(LocalDateTime date);

    // Find quizzes with score above threshold
    @Query("SELECT q FROM Quiz q WHERE q.totalScore >= :minScore")
    List<Quiz> findQuizzesWithMinScore(@Param("minScore") Integer minScore);

    // Find recent quizzes for a caregiver
    List<Quiz> findByCaregiverIdOrderByDateTakenDesc(Long caregiverId);

    // Count quizzes by caregiver
    long countByCaregiverId(Long caregiverId);

    // Find quizzes by date range
    List<Quiz> findByDateTakenBetween(LocalDateTime startDate, LocalDateTime endDate);
}
