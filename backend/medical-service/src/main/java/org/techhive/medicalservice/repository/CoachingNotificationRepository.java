package org.techhive.medicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.medicalservice.entity.coaching.CoachingNotification;

import java.util.List;

public interface CoachingNotificationRepository extends JpaRepository<CoachingNotification, Long> {
    List<CoachingNotification> findByRecipientUserIdOrderByCreatedAtDesc(String recipientUserId);

    long countByRecipientUserIdAndReadFalse(String recipientUserId);

    void deleteByCoachingGoal_Id(Long coachingGoalId);
}
