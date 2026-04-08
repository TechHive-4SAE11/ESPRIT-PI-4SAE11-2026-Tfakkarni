package org.techhive.medicalservice.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.medicalservice.entity.coaching.CoachingProgress;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CoachingProgressRepository extends JpaRepository<CoachingProgress, Long> {

    List<CoachingProgress> findByCoachingGoalIdOrderByDateRecordedDesc(Long coachingGoalId, Pageable pageable);

    List<CoachingProgress> findByCoachingGoalIdOrderByDateRecordedDesc(Long coachingGoalId);

    @Query("SELECT MAX(p.dateRecorded) FROM CoachingProgress p WHERE p.coachingGoal.id = :goalId")
    Optional<LocalDate> findLatestProgressDate(@Param("goalId") Long goalId);

    void deleteByCoachingGoalId(Long coachingGoalId);
}
