package org.techhive.gameservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.*;
import org.techhive.gameservice.repository.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomGameServiceTest {

    @Mock
    private CustomGameRepository gameRepo;
    @Mock
    private CustomGameAttemptRepository attemptRepo;
    @Mock
    private PhotoMemoryRepository photoRepo;
    @Mock
    private PlaceMemoryRepository placeRepo;
    @Mock
    private MovieMemoryRepository movieRepo;
    @Mock
    private QuestionMemoryRepository questionRepo;
    @Mock
    private MemoryTagRepository tagRepo;
    @Mock
    private DataPointPerformanceRepository perfRepo;
    @Mock
    private PatientContextService patientContextService;

    @InjectMocks
    private CustomGameService customGameService;

    private static final String PATIENT_ID = "patient-abc";

    @Test
    void createGame_savesAndReturnsResponse() {
        CreateCustomGameRequest req = new CreateCustomGameRequest();
        req.setTitle("Memory Mix");
        req.setDescription("Mixed game");
        req.setItems(List.of(
                new CreateCustomGameRequest.GameItemEntry(DataPointType.PHOTO, 1L),
                new CreateCustomGameRequest.GameItemEntry(DataPointType.QUESTION, 2L)
        ));

        CustomGame saved = new CustomGame();
        saved.setId(1L);
        saved.setPatientKeycloakId(PATIENT_ID);
        saved.setTitle("Memory Mix");
        saved.setDescription("Mixed game");
        saved.setCreatedAt(LocalDateTime.now());
        saved.setItems(new ArrayList<>());

        when(gameRepo.save(any(CustomGame.class))).thenReturn(saved);

        CustomGameResponse result = customGameService.createGame(PATIENT_ID, req);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Memory Mix");
        verify(gameRepo).save(any(CustomGame.class));
    }

    @Test
    void updateGame_updatesFieldsAndItems() {
        CustomGame existing = new CustomGame();
        existing.setId(1L);
        existing.setPatientKeycloakId(PATIENT_ID);
        existing.setTitle("Old Title");
        existing.setItems(new ArrayList<>());
        existing.setCreatedAt(LocalDateTime.now());

        EditCustomGameRequest req = new EditCustomGameRequest();
        req.setTitle("New Title");
        req.setDescription("Updated desc");
        req.setItems(List.of(
                new EditCustomGameRequest.GameItemEntry(DataPointType.MOVIE, 5L)
        ));

        when(gameRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(gameRepo.save(any(CustomGame.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomGameResponse result = customGameService.updateGame(1L, req);

        assertThat(result.getTitle()).isEqualTo("New Title");
        verify(gameRepo).save(any(CustomGame.class));
    }

    @Test
    void updateGame_notFound_throwsException() {
        when(gameRepo.findById(999L)).thenReturn(Optional.empty());

        EditCustomGameRequest req = new EditCustomGameRequest();
        req.setTitle("x");
        req.setItems(List.of());

        assertThatThrownBy(() -> customGameService.updateGame(999L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    void getGamesForPatient_returnsMapped() {
        CustomGame g1 = new CustomGame();
        g1.setId(1L);
        g1.setPatientKeycloakId(PATIENT_ID);
        g1.setTitle("Game 1");
        g1.setCreatedAt(LocalDateTime.now());
        g1.setItems(new ArrayList<>());

        when(gameRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(g1));

        List<CustomGameResponse> result = customGameService.getGamesForPatient(PATIENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Game 1");
    }

    @Test
    void getPlayData_returnsPlayItems() {
        CustomGame game = new CustomGame();
        game.setId(1L);
        game.setPatientKeycloakId(PATIENT_ID);
        game.setTitle("Test Game");

        CustomGameItem item = new CustomGameItem(game, DataPointType.QUESTION, 10L, 0);
        item.setId(100L);
        game.setItems(new ArrayList<>(List.of(item)));

        QuestionMemory qm = new QuestionMemory();
        qm.setId(10L);
        qm.setQuestionText("What is your pet's name?");
        qm.setCorrectAnswer("Rex");

        when(gameRepo.findById(1L)).thenReturn(Optional.of(game));
        when(questionRepo.findById(10L)).thenReturn(Optional.of(qm));

        UnifiedPlayData result = customGameService.getPlayData(1L);

        assertThat(result.getGameId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Game");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getType()).isEqualTo(DataPointType.QUESTION);
        assertThat(result.getItems().get(0).getCorrectAnswer()).isEqualTo("Rex");
    }

    @Test
    void getPlayData_gameNotFound_throwsException() {
        when(gameRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customGameService.getPlayData(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    void getRandomPlayData_prioritizesNewItems() {
        // Set up: 2 questions exist, no performance records = all Tier 1 (new)
        QuestionMemory q1 = new QuestionMemory();
        q1.setId(1L);
        q1.setQuestionText("Q1?");
        q1.setCorrectAnswer("A1");

        QuestionMemory q2 = new QuestionMemory();
        q2.setId(2L);
        q2.setQuestionText("Q2?");
        q2.setCorrectAnswer("A2");

        when(photoRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of());
        when(placeRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of());
        when(movieRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of());
        when(questionRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(q1, q2));
        when(perfRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of());
        when(questionRepo.findById(1L)).thenReturn(Optional.of(q1));
        when(questionRepo.findById(2L)).thenReturn(Optional.of(q2));

        UnifiedPlayData result = customGameService.getRandomPlayData(PATIENT_ID, 10);

        assertThat(result.getTitle()).isEqualTo("Random Memory Mix");
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    void getRandomPlayData_respectsLimit() {
        QuestionMemory q1 = new QuestionMemory();
        q1.setId(1L);
        q1.setQuestionText("Q1?");
        q1.setCorrectAnswer("A1");

        QuestionMemory q2 = new QuestionMemory();
        q2.setId(2L);
        q2.setQuestionText("Q2?");
        q2.setCorrectAnswer("A2");

        when(photoRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of());
        when(placeRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of());
        when(movieRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of());
        when(questionRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(q1, q2));
        when(perfRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of());
        when(questionRepo.findById(anyLong())).thenReturn(Optional.of(q1));

        UnifiedPlayData result = customGameService.getRandomPlayData(PATIENT_ID, 1);

        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void submitResults_savesAttemptAndReturnsResult() {
        UnifiedSubmitRequest req = new UnifiedSubmitRequest();
        req.setGameId(1L);
        req.setTotalQuestions(1);
        req.setDurationSeconds(30);
        req.setAnswers(List.of()); // no answers to validate

        CustomGame game = new CustomGame();
        game.setId(1L);
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game));

        CustomGameAttempt savedAttempt = new CustomGameAttempt();
        savedAttempt.setId(100L);
        savedAttempt.setScore(0);
        savedAttempt.setTotalQuestions(1);
        savedAttempt.setCompletedAt(LocalDateTime.now());
        when(attemptRepo.save(any(CustomGameAttempt.class))).thenReturn(savedAttempt);

        UnifiedPlayResult result = customGameService.submitResults(PATIENT_ID, req);

        assertThat(result.getAttemptId()).isEqualTo(100L);
        assertThat(result.getTotalQuestions()).isEqualTo(1);
        verify(attemptRepo).save(any(CustomGameAttempt.class));
    }

    @Test
    void submitResults_withNullGameId_worksForRandomGames() {
        UnifiedSubmitRequest req = new UnifiedSubmitRequest();
        req.setGameId(null); // random game, no specific game
        req.setTotalQuestions(0);
        req.setDurationSeconds(10);
        req.setAnswers(List.of());

        CustomGameAttempt savedAttempt = new CustomGameAttempt();
        savedAttempt.setId(200L);
        savedAttempt.setScore(0);
        savedAttempt.setTotalQuestions(0);
        savedAttempt.setCompletedAt(LocalDateTime.now());
        when(attemptRepo.save(any(CustomGameAttempt.class))).thenReturn(savedAttempt);

        UnifiedPlayResult result = customGameService.submitResults(PATIENT_ID, req);

        assertThat(result.getAttemptId()).isEqualTo(200L);
    }

    @Test
    void deleteGame_callsRepository() {
        customGameService.deleteGame(1L);
        verify(gameRepo).deleteById(1L);
    }

    @Test
    void getStats_returnsCorrectCounts() {
        when(attemptRepo.countByPlayerKeycloakId(PATIENT_ID)).thenReturn(5L);
        when(attemptRepo.getAverageScorePercentage(PATIENT_ID)).thenReturn(75.0);
        when(attemptRepo.getBestScore(PATIENT_ID)).thenReturn(90);
        when(photoRepo.countByPatientKeycloakId(PATIENT_ID)).thenReturn(3L);
        when(placeRepo.countByPatientKeycloakId(PATIENT_ID)).thenReturn(2L);
        when(movieRepo.countByPatientKeycloakId(PATIENT_ID)).thenReturn(4L);
        when(questionRepo.countByPatientKeycloakId(PATIENT_ID)).thenReturn(6L);

        Map<String, Object> stats = customGameService.getStats(PATIENT_ID);

        assertThat(stats.get("totalGamesPlayed")).isEqualTo(5L);
        assertThat(stats.get("averageScore")).isEqualTo(75.0);
        assertThat(stats.get("bestScore")).isEqualTo(90);
        assertThat(stats.get("photoCount")).isEqualTo(3L);
        assertThat(stats.get("questionCount")).isEqualTo(6L);
    }

    @Test
    void getStats_noAttempts_returnsZeros() {
        when(attemptRepo.countByPlayerKeycloakId(PATIENT_ID)).thenReturn(0L);
        when(photoRepo.countByPatientKeycloakId(PATIENT_ID)).thenReturn(0L);
        when(placeRepo.countByPatientKeycloakId(PATIENT_ID)).thenReturn(0L);
        when(movieRepo.countByPatientKeycloakId(PATIENT_ID)).thenReturn(0L);
        when(questionRepo.countByPatientKeycloakId(PATIENT_ID)).thenReturn(0L);

        Map<String, Object> stats = customGameService.getStats(PATIENT_ID);

        assertThat(stats.get("totalGamesPlayed")).isEqualTo(0L);
        assertThat(stats.get("averageScore")).isEqualTo(0.0);
        assertThat(stats.get("bestScore")).isEqualTo(0);
    }
}
