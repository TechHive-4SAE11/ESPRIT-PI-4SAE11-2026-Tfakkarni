package org.techhive.medicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.medicalservice.entity.coaching.CoachingGoal;
import org.techhive.medicalservice.entity.coaching.CoachingGoalStatus;

import java.util.List;

public interface CoachingGoalRepository extends JpaRepository<CoachingGoal, Long> {

    List<CoachingGoal> findByMedicalFolder_IdOrderByCreatedAtDesc(Long medicalFolderId);

    List<CoachingGoal> findByStatus(CoachingGoalStatus status);
}
