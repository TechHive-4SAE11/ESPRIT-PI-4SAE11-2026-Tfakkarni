package org.techhive.medicalservice.service.coaching;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.techhive.medicalservice.dto.coaching.*;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.coaching.*;
import org.techhive.medicalservice.repository.CoachingGoalRepository;
import org.techhive.medicalservice.repository.CoachingNotificationRepository;
import org.techhive.medicalservice.repository.CoachingProgressRepository;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachingServiceImpl implements CoachingService {

    private final CoachingGoalRepository coachingGoalRepository;
    private final CoachingProgressRepository coachingProgressRepository;
    private final MedicalFolderRepository medicalFolderRepository;
    private final DiagnosticsRepository diagnosticsRepository;
    private final OpenMeteoClient openMeteoClient;
    private final CoachingNotificationService coachingNotificationService;
    private final CoachingNotificationRepository coachingNotificationRepository;

    @Override
    @Transactional
    public CoachingGoalResponse createGoal(Long folderId, CoachingGoalRequest request, String doctorId) {
        MedicalFolder folder = medicalFolderRepository.findById(folderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        Diagnostics diag = null;
        if (request.getDiagnosticId() != null) {
            diag = diagnosticsRepository.findById(request.getDiagnosticId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnostic not found"));
            if (!diag.getMedicalFolder().getId().equals(folderId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnostic does not belong to folder");
            }
        }
        CoachingPriority p = request.getPriority() != null ? request.getPriority() : CoachingPriority.MEDIUM;
        CoachingGoal goal = CoachingGoal.builder()
                .medicalFolder(folder)
                .diagnostics(diag)
                .goalType(request.getGoalType())
                .goalTitle(request.getGoalTitle().trim())
                .actionSteps(request.getActionSteps())
                .tips(request.getTips())
                .targetDays(request.getTargetDays())
                .status(CoachingGoalStatus.ACTIVE)
                .priority(p)
                .outdoorActivity(request.isOutdoorActivity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .createdByDoctorId(doctorId)
                .build();
        goal = coachingGoalRepository.save(goal);
        coachingNotificationService.notifyUser(
                folder.getPatientId(),
                folder,
                goal,
                "GOAL_CREATED",
                "Nouveau coaching",
                "Votre medecin a ajoute un objectif: " + goal.getGoalTitle());
        return toGoalResponse(goal);
    }

    @Override
    @Transactional
    public CoachingGoalResponse updateGoal(Long folderId, Long goalId, CoachingGoalRequest request) {
        CoachingGoal goal = loadGoal(folderId, goalId);
        if (request.getDiagnosticId() != null) {
            Diagnostics diag = diagnosticsRepository.findById(request.getDiagnosticId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnostic not found"));
            if (!diag.getMedicalFolder().getId().equals(folderId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnostic does not belong to folder");
            }
            goal.setDiagnostics(diag);
        } else {
            goal.setDiagnostics(null);
        }
        goal.setGoalType(request.getGoalType());
        goal.setGoalTitle(request.getGoalTitle().trim());
        goal.setActionSteps(request.getActionSteps());
        goal.setTips(request.getTips());
        goal.setTargetDays(request.getTargetDays());
        if (request.getPriority() != null) {
            goal.setPriority(request.getPriority());
        }
        goal.setOutdoorActivity(request.isOutdoorActivity());
        goal.setLatitude(request.getLatitude());
        goal.setLongitude(request.getLongitude());
        CoachingGoal saved = coachingGoalRepository.save(goal);
        coachingNotificationService.notifyUser(
                goal.getMedicalFolder().getPatientId(),
                goal.getMedicalFolder(),
                saved,
                "GOAL_UPDATED",
                "Objectif coaching mis a jour",
                "Le medecin a mis a jour l'objectif: " + saved.getGoalTitle());
        return toGoalResponse(saved);
    }

    @Override
    @Transactional
    public CoachingGoalResponse patchGoalStatus(Long folderId, Long goalId, CoachingGoalStatus status) {
        CoachingGoal goal = loadGoal(folderId, goalId);
        goal.setStatus(status);
        CoachingGoal saved = coachingGoalRepository.save(goal);
        coachingNotificationService.notifyUser(
                goal.getMedicalFolder().getPatientId(),
                goal.getMedicalFolder(),
                saved,
                "GOAL_STATUS_CHANGED",
                "Statut de l'objectif modifie",
                "Objectif \"" + saved.getGoalTitle() + "\" : " + saved.getStatus());
        return toGoalResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachingGoalResponse> listGoals(Long folderId) {
        assertFolder(folderId);
        return coachingGoalRepository.findByMedicalFolder_IdOrderByCreatedAtDesc(folderId).stream()
                .map(this::toGoalResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CoachingGoalResponse getGoal(Long folderId, Long goalId) {
        return toGoalResponse(loadGoal(folderId, goalId));
    }

    @Override
    @Transactional
    public void deleteGoal(Long folderId, Long goalId) {
        CoachingGoal goal = loadGoal(folderId, goalId);
        MedicalFolder folder = goal.getMedicalFolder();
        String title = goal.getGoalTitle();
        coachingProgressRepository.deleteByCoachingGoalId(goalId);
        coachingProgressRepository.flush();
        coachingNotificationRepository.deleteByCoachingGoal_Id(goalId);
        coachingNotificationRepository.flush();
        coachingGoalRepository.delete(goal);
        coachingGoalRepository.flush();
        coachingNotificationService.notifyUser(
                folder.getPatientId(),
                folder,
                null,
                "GOAL_DELETED",
                "Objectif coaching supprime",
                "Le medecin a supprime l'objectif: " + title);
    }

    @Override
    @Transactional
    public CoachingProgressResponse addProgress(Long folderId, Long goalId, CoachingProgressRequest request,
            String recordedByUserId) {
        CoachingGoal goal = loadGoal(folderId, goalId);
        LocalDate date = request.getDateRecorded() != null ? request.getDateRecorded() : LocalDate.now();

        CoachingProgress progress = CoachingProgress.builder()
                .coachingGoal(goal)
                .dateRecorded(date)
                .completionPercentage(request.getCompletionPercentage())
                .mood(request.getMood())
                .energyLevel(request.getEnergyLevel())
                .helperNotes(request.getHelperNotes())
                .patientFeedback(request.getPatientFeedback())
                .recordedByRole(request.getRecordedByRole())
                .recordedByUserId(recordedByUserId)
                .build();

        if (goal.isOutdoorActivity() && goal.getLatitude() != null && goal.getLongitude() != null) {
            openMeteoClient.fetchCurrentSummary(goal.getLatitude(), goal.getLongitude()).ifPresent(summary -> {
                progress.setWeatherSummary(summary);
                progress.setWeatherFetchedAt(LocalDateTime.now());
            });
        }

        final CoachingProgress saved = coachingProgressRepository.save(progress);

        MedicalFolder folder = goal.getMedicalFolder();
        coachingNotificationService.notifyUser(
                folder.getPatientId(),
                folder,
                goal,
                "PROGRESS_LOGGED",
                "Suivi coaching enregistré",
                "Nouveau suivi pour: " + goal.getGoalTitle());

        coachingNotificationService.notifyUser(
                folder.getDoctorId(),
                folder,
                goal,
                "PROGRESS_LOGGED_FOR_DOCTOR",
                "Nouveau suivi coaching",
                "Un suivi a ete enregistre pour: " + goal.getGoalTitle());

        return toProgressResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachingProgressResponse> listProgress(Long folderId, Long goalId) {
        loadGoal(folderId, goalId);
        return coachingProgressRepository.findByCoachingGoalIdOrderByDateRecordedDesc(goalId).stream()
                .map(this::toProgressResponse)
                .collect(Collectors.toList());
    }

    private void assertFolder(Long folderId) {
        if (!medicalFolderRepository.existsById(folderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found");
        }
    }

    private CoachingGoal loadGoal(Long folderId, Long goalId) {
        CoachingGoal goal = coachingGoalRepository.findById(goalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found"));
        if (!goal.getMedicalFolder().getId().equals(folderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not in folder");
        }
        return goal;
    }

    private CoachingGoalResponse toGoalResponse(CoachingGoal g) {
        return CoachingGoalResponse.builder()
                .id(g.getId())
                .medicalFolderId(g.getMedicalFolder().getId())
                .diagnosticId(g.getDiagnostics() != null ? g.getDiagnostics().getId() : null)
                .goalType(g.getGoalType())
                .goalTitle(g.getGoalTitle())
                .actionSteps(g.getActionSteps())
                .tips(g.getTips())
                .targetDays(g.getTargetDays())
                .status(g.getStatus())
                .priority(g.getPriority())
                .outdoorActivity(g.isOutdoorActivity())
                .latitude(g.getLatitude())
                .longitude(g.getLongitude())
                .createdByDoctorId(g.getCreatedByDoctorId())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .lastStaleNotificationAt(g.getLastStaleNotificationAt())
                .build();
    }

    private CoachingProgressResponse toProgressResponse(CoachingProgress p) {
        return CoachingProgressResponse.builder()
                .id(p.getId())
                .coachingGoalId(p.getCoachingGoal().getId())
                .dateRecorded(p.getDateRecorded())
                .completionPercentage(p.getCompletionPercentage())
                .mood(p.getMood())
                .energyLevel(p.getEnergyLevel())
                .helperNotes(p.getHelperNotes())
                .patientFeedback(p.getPatientFeedback())
                .recordedByRole(p.getRecordedByRole())
                .recordedByUserId(p.getRecordedByUserId())
                .weatherSummary(p.getWeatherSummary())
                .weatherFetchedAt(p.getWeatherFetchedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
