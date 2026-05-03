package org.techhive.medicalservice.service.coaching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.techhive.medicalservice.dto.coaching.CoachingGoalRequest;
import org.techhive.medicalservice.dto.coaching.CoachingGoalResponse;
import org.techhive.medicalservice.dto.coaching.CoachingProgressRequest;
import org.techhive.medicalservice.dto.coaching.CoachingProgressResponse;
import org.techhive.medicalservice.entity.Diagnostics;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.coaching.CoachingGoal;
import org.techhive.medicalservice.entity.coaching.CoachingGoalStatus;
import org.techhive.medicalservice.entity.coaching.CoachingGoalType;
import org.techhive.medicalservice.entity.coaching.CoachingMood;
import org.techhive.medicalservice.entity.coaching.CoachingPriority;
import org.techhive.medicalservice.entity.coaching.CoachingProgress;
import org.techhive.medicalservice.entity.coaching.ProgressRecordedByRole;
import org.techhive.medicalservice.repository.CoachingGoalRepository;
import org.techhive.medicalservice.repository.CoachingNotificationRepository;
import org.techhive.medicalservice.repository.CoachingProgressRepository;
import org.techhive.medicalservice.repository.DiagnosticsRepository;
import org.techhive.medicalservice.repository.MedicalFolderRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoachingServiceImplTest {

    @Mock
    private CoachingGoalRepository coachingGoalRepository;
    @Mock
    private CoachingProgressRepository coachingProgressRepository;
    @Mock
    private MedicalFolderRepository medicalFolderRepository;
    @Mock
    private DiagnosticsRepository diagnosticsRepository;
    @Mock
    private OpenMeteoClient openMeteoClient;
    @Mock
    private CoachingNotificationService coachingNotificationService;
    @Mock
    private CoachingNotificationRepository coachingNotificationRepository;

    private CoachingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CoachingServiceImpl(
                coachingGoalRepository,
                coachingProgressRepository,
                medicalFolderRepository,
                diagnosticsRepository,
                openMeteoClient,
                coachingNotificationService,
                coachingNotificationRepository);
    }

    @Test
    void createGoalValidatesFolderDiagnosticOwnershipAndCreatesDefaultPriorityGoal() {
        CoachingGoalRequest request = goalRequest(null, null);
        when(medicalFolderRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException missingFolder = assertThrows(ResponseStatusException.class,
                () -> service.createGoal(404L, request, "doctor-a"));
        assertEquals(HttpStatus.NOT_FOUND, missingFolder.getStatusCode());

        MedicalFolder folder = folder(10L, "patient-a", "doctor-a");
        when(medicalFolderRepository.findById(10L)).thenReturn(Optional.of(folder));
        CoachingGoal saved = goal(100L, folder, null);
        when(coachingGoalRepository.save(any(CoachingGoal.class))).thenReturn(saved);

        CoachingGoalResponse response = service.createGoal(10L, request, "doctor-a");

        assertEquals(100L, response.getId());
        assertEquals(10L, response.getMedicalFolderId());
        assertEquals(CoachingPriority.MEDIUM, response.getPriority());
        assertEquals(CoachingGoalStatus.ACTIVE, response.getStatus());
        verify(coachingNotificationService).notifyUser(
                eq("patient-a"), eq(folder), eq(saved), eq("GOAL_CREATED"), anyString(), contains("Walk daily"));
    }

    @Test
    void createAndUpdateGoalRejectBadDiagnosticsAndUpdateMutableFields() {
        MedicalFolder folder = folder(10L, "patient-a", "doctor-a");
        MedicalFolder otherFolder = folder(11L, "patient-b", "doctor-b");
        CoachingGoalRequest request = goalRequest(5L, CoachingPriority.HIGH);
        when(medicalFolderRepository.findById(10L)).thenReturn(Optional.of(folder));
        when(diagnosticsRepository.findById(5L)).thenReturn(Optional.empty());

        ResponseStatusException missingDiagnostic = assertThrows(ResponseStatusException.class,
                () -> service.createGoal(10L, request, "doctor-a"));
        assertEquals(HttpStatus.BAD_REQUEST, missingDiagnostic.getStatusCode());

        Diagnostics wrongDiagnostic = Diagnostics.builder().id(5L).medicalFolder(otherFolder).build();
        when(diagnosticsRepository.findById(5L)).thenReturn(Optional.of(wrongDiagnostic));
        ResponseStatusException wrongFolder = assertThrows(ResponseStatusException.class,
                () -> service.createGoal(10L, request, "doctor-a"));
        assertEquals(HttpStatus.BAD_REQUEST, wrongFolder.getStatusCode());

        Diagnostics diagnostic = Diagnostics.builder().id(5L).medicalFolder(folder).build();
        CoachingGoal existing = goal(100L, folder, null);
        when(coachingGoalRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(diagnosticsRepository.findById(5L)).thenReturn(Optional.of(diagnostic));
        when(coachingGoalRepository.save(existing)).thenReturn(existing);

        CoachingGoalResponse updated = service.updateGoal(10L, 100L, request);

        assertEquals(5L, updated.getDiagnosticId());
        assertEquals(CoachingPriority.HIGH, updated.getPriority());
        assertTrue(updated.isOutdoorActivity());
        assertEquals(36.8, updated.getLatitude());
        verify(coachingNotificationService).notifyUser(
                eq("patient-a"), eq(folder), eq(existing), eq("GOAL_UPDATED"), anyString(), contains("Walk daily"));
    }

    @Test
    void goalReadStatusListAndDeletePathsUseRepositoriesAndNotifications() {
        MedicalFolder folder = folder(10L, "patient-a", "doctor-a");
        CoachingGoal goal = goal(100L, folder, null);
        when(coachingGoalRepository.findById(100L)).thenReturn(Optional.of(goal));
        when(coachingGoalRepository.save(goal)).thenReturn(goal);

        CoachingGoalResponse patched = service.patchGoalStatus(10L, 100L, CoachingGoalStatus.COMPLETED);

        assertEquals(CoachingGoalStatus.COMPLETED, patched.getStatus());
        verify(coachingNotificationService).notifyUser(
                eq("patient-a"), eq(folder), eq(goal), eq("GOAL_STATUS_CHANGED"), anyString(), contains("COMPLETED"));

        when(medicalFolderRepository.existsById(10L)).thenReturn(true);
        when(coachingGoalRepository.findByMedicalFolder_IdOrderByCreatedAtDesc(10L)).thenReturn(List.of(goal));
        assertEquals(1, service.listGoals(10L).size());
        assertEquals(100L, service.getGoal(10L, 100L).getId());

        service.deleteGoal(10L, 100L);

        verify(coachingProgressRepository).deleteByCoachingGoalId(100L);
        verify(coachingProgressRepository).flush();
        verify(coachingNotificationRepository).deleteByCoachingGoal_Id(100L);
        verify(coachingNotificationRepository).flush();
        verify(coachingGoalRepository).delete(goal);
        verify(coachingGoalRepository).flush();
        verify(coachingNotificationService).notifyUser(
                eq("patient-a"), eq(folder), isNull(), eq("GOAL_DELETED"), anyString(), contains("Walk daily"));
    }

    @Test
    void listGoalsAndLoadGoalRejectMissingFolderMissingGoalAndWrongFolder() {
        when(medicalFolderRepository.existsById(404L)).thenReturn(false);
        ResponseStatusException missingFolder = assertThrows(ResponseStatusException.class, () -> service.listGoals(404L));
        assertEquals(HttpStatus.NOT_FOUND, missingFolder.getStatusCode());

        when(coachingGoalRepository.findById(404L)).thenReturn(Optional.empty());
        ResponseStatusException missingGoal = assertThrows(ResponseStatusException.class, () -> service.getGoal(10L, 404L));
        assertEquals(HttpStatus.NOT_FOUND, missingGoal.getStatusCode());

        CoachingGoal otherFolderGoal = goal(100L, folder(99L, "patient-x", "doctor-x"), null);
        when(coachingGoalRepository.findById(100L)).thenReturn(Optional.of(otherFolderGoal));
        ResponseStatusException wrongFolder = assertThrows(ResponseStatusException.class, () -> service.getGoal(10L, 100L));
        assertEquals(HttpStatus.NOT_FOUND, wrongFolder.getStatusCode());
    }

    @Test
    void addProgressRecordsWeatherWhenOutdoorAndNotifiesPatientAndDoctor() {
        MedicalFolder folder = folder(10L, "patient-a", "doctor-a");
        CoachingGoal goal = goal(100L, folder, null);
        goal.setOutdoorActivity(true);
        goal.setLatitude(36.8);
        goal.setLongitude(10.2);
        CoachingProgressRequest request = CoachingProgressRequest.builder()
                .dateRecorded(LocalDate.of(2026, 5, 3))
                .completionPercentage(75)
                .mood(CoachingMood.GOOD)
                .energyLevel(8)
                .helperNotes("walk completed")
                .patientFeedback("felt calm")
                .recordedByRole(ProgressRecordedByRole.HELPER)
                .build();
        when(coachingGoalRepository.findById(100L)).thenReturn(Optional.of(goal));
        when(openMeteoClient.fetchCurrentSummary(36.8, 10.2)).thenReturn(Optional.of("22°C (code météo 1)"));
        when(coachingProgressRepository.save(any(CoachingProgress.class))).thenAnswer(invocation -> {
            CoachingProgress progress = invocation.getArgument(0);
            progress.setId(200L);
            progress.setCreatedAt(LocalDateTime.of(2026, 5, 3, 9, 0));
            return progress;
        });

        CoachingProgressResponse response = service.addProgress(10L, 100L, request, "helper-a");

        assertEquals(200L, response.getId());
        assertEquals(100L, response.getCoachingGoalId());
        assertEquals(LocalDate.of(2026, 5, 3), response.getDateRecorded());
        assertEquals(75, response.getCompletionPercentage());
        assertEquals("22°C (code météo 1)", response.getWeatherSummary());
        assertNotNull(response.getWeatherFetchedAt());
        verify(coachingNotificationService).notifyUser(
                eq("patient-a"), eq(folder), eq(goal), eq("PROGRESS_LOGGED"), anyString(), contains("Walk daily"));
        verify(coachingNotificationService).notifyUser(
                eq("doctor-a"), eq(folder), eq(goal), eq("PROGRESS_LOGGED_FOR_DOCTOR"), anyString(), contains("Walk daily"));
    }

    @Test
    void addProgressUsesTodayWithoutWeatherAndListProgressMapsExistingRows() {
        MedicalFolder folder = folder(10L, "patient-a", "doctor-a");
        CoachingGoal goal = goal(100L, folder, null);
        CoachingProgressRequest request = CoachingProgressRequest.builder()
                .completionPercentage(40)
                .mood(CoachingMood.NEUTRAL)
                .energyLevel(5)
                .recordedByRole(ProgressRecordedByRole.PATIENT)
                .build();
        when(coachingGoalRepository.findById(100L)).thenReturn(Optional.of(goal));
        when(coachingProgressRepository.save(any(CoachingProgress.class))).thenAnswer(invocation -> {
            CoachingProgress progress = invocation.getArgument(0);
            progress.setId(201L);
            return progress;
        });

        CoachingProgressResponse response = service.addProgress(10L, 100L, request, "patient-a");

        assertEquals(LocalDate.now(), response.getDateRecorded());
        assertNull(response.getWeatherSummary());
        verifyNoInteractions(openMeteoClient);

        CoachingProgress progress = CoachingProgress.builder()
                .id(202L)
                .coachingGoal(goal)
                .dateRecorded(LocalDate.of(2026, 5, 2))
                .completionPercentage(55)
                .mood(CoachingMood.LOW)
                .energyLevel(3)
                .recordedByRole(ProgressRecordedByRole.HELPER)
                .recordedByUserId("helper-a")
                .createdAt(LocalDateTime.of(2026, 5, 2, 10, 0))
                .build();
        when(coachingProgressRepository.findByCoachingGoalIdOrderByDateRecordedDesc(100L)).thenReturn(List.of(progress));

        List<CoachingProgressResponse> progressRows = service.listProgress(10L, 100L);

        assertEquals(1, progressRows.size());
        assertEquals(202L, progressRows.get(0).getId());
        assertEquals(CoachingMood.LOW, progressRows.get(0).getMood());
    }

    private static CoachingGoalRequest goalRequest(Long diagnosticId, CoachingPriority priority) {
        return CoachingGoalRequest.builder()
                .diagnosticId(diagnosticId)
                .goalType(CoachingGoalType.ACTIVITY_INCREASE)
                .goalTitle("  Walk daily  ")
                .actionSteps("Walk 15 minutes with helper")
                .tips("Use safe familiar route")
                .targetDays(14)
                .priority(priority)
                .outdoorActivity(true)
                .latitude(36.8)
                .longitude(10.2)
                .build();
    }

    private static MedicalFolder folder(Long id, String patientId, String doctorId) {
        return MedicalFolder.builder()
                .id(id)
                .patientId(patientId)
                .doctorId(doctorId)
                .build();
    }

    private static CoachingGoal goal(Long id, MedicalFolder folder, Diagnostics diagnostics) {
        return CoachingGoal.builder()
                .id(id)
                .medicalFolder(folder)
                .diagnostics(diagnostics)
                .goalType(CoachingGoalType.ACTIVITY_INCREASE)
                .goalTitle("Walk daily")
                .actionSteps("Walk 15 minutes with helper")
                .tips("Use safe familiar route")
                .targetDays(14)
                .status(CoachingGoalStatus.ACTIVE)
                .priority(CoachingPriority.MEDIUM)
                .outdoorActivity(false)
                .createdByDoctorId("doctor-a")
                .createdAt(LocalDateTime.of(2026, 5, 3, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 3, 8, 30))
                .build();
    }
}
