package org.techhive.medicalservice.service.coaching;

import org.techhive.medicalservice.dto.coaching.*;
import org.techhive.medicalservice.entity.coaching.CoachingGoalStatus;

import java.util.List;

public interface CoachingService {

    CoachingGoalResponse createGoal(Long folderId, CoachingGoalRequest request, String doctorId);

    CoachingGoalResponse updateGoal(Long folderId, Long goalId, CoachingGoalRequest request);

    CoachingGoalResponse patchGoalStatus(Long folderId, Long goalId, CoachingGoalStatus status);

    List<CoachingGoalResponse> listGoals(Long folderId);

    CoachingGoalResponse getGoal(Long folderId, Long goalId);

    void deleteGoal(Long folderId, Long goalId);

    CoachingProgressResponse addProgress(Long folderId, Long goalId, CoachingProgressRequest request, String recordedByUserId);

    List<CoachingProgressResponse> listProgress(Long folderId, Long goalId);
}
