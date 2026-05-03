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
                new CreateCustomGameRequest.GameItemEntry(DataPointType.QUESTION, 2L)));

        CustomGame saved = customGame(1L, "Memory Mix", List.of());
        when(gameRepo.save(any(CustomGame.class))).thenAnswer(invocation -> {
            CustomGame game = invocation.getArgument(0);
            assertThat(game.getItems()).hasSize(2);
            game.setId(1L);
            game.setCreatedAt(LocalDateTime.now());
            return game;
        });

        CustomGameResponse result = customGameService.createGame(PATIENT_ID, req);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Memory Mix");
        assertThat(result.getItemCount()).isEqualTo(2);
        verify(gameRepo).save(any(CustomGame.class));
    }

    @Test
    void updateGame_updatesFieldsAndItems() {
        CustomGame existing = customGame(1L, "Old Title", new ArrayList<>());
        EditCustomGameRequest req = new EditCustomGameRequest();
        req.setTitle("New Title");
        req.setDescription("Updated desc");
        req.setItems(List.of(new EditCustomGameRequest.GameItemEntry(DataPointType.MOVIE, 5L)));

        when(gameRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(gameRepo.save(any(CustomGame.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomGameResponse result = customGameService.updateGame(1L, req);

        assertThat(result.getTitle()).isEqualTo("New Title");
        assertThat(result.getDescription()).isEqualTo("Updated desc");
        assertThat(result.getItemCount()).isEqualTo(1);
        assertThat(existing.getItems().get(0).getDataType()).isEqualTo(DataPointType.MOVIE);
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
        CustomGame g1 = customGame(1L, "Game 1", List.of(new CustomGameItem(null, DataPointType.PHOTO, 1L, 0)));
        when(gameRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(g1));

        List<CustomGameResponse> result = customGameService.getGamesForPatient(PATIENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Game 1");
        assertThat(result.get(0).getItemTypes()).containsExactly(DataPointType.PHOTO);
    }

    @Test
    void getGameDetail_resolvesEverySupportedDataPointType() {
        CustomGame game = customGame(1L, "Detailed Game", List.of(
                new CustomGameItem(null, DataPointType.PHOTO, 10L, 0),
                new CustomGameItem(null, DataPointType.PLACE, 20L, 1),
                new CustomGameItem(null, DataPointType.MOVIE, 30L, 2),
                new CustomGameItem(null, DataPointType.QUESTION, 40L, 3)));
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game));
        when(photoRepo.findById(10L)).thenReturn(Optional.of(photo(10L, "Sami")));
        when(placeRepo.findById(20L)).thenReturn(Optional.of(place(20L, "Home")));
        when(movieRepo.findById(30L)).thenReturn(Optional.of(movie(30L, "Interstellar", "Cooper")));
        when(questionRepo.findById(40L)).thenReturn(Optional.of(question(40L, "Pet?", "Milo")));

        CustomGameDetailResponse detail = customGameService.getGameDetail(1L);

        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getItems()).extracting(DataPointSummary::getType)
                .containsExactly(DataPointType.PHOTO, DataPointType.PLACE, DataPointType.MOVIE, DataPointType.QUESTION);
        assertThat(detail.getItems()).extracting(DataPointSummary::getLabel)
                .contains("Sami", "Home", "Interstellar", "Pet?");
    }

    @Test
    void getGameDetail_filtersMissingDataPointsAndThrowsWhenGameMissing() {
        CustomGame game = customGame(1L, "Partial", List.of(new CustomGameItem(null, DataPointType.PHOTO, 10L, 0)));
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game));
        when(photoRepo.findById(10L)).thenReturn(Optional.empty());
        when(gameRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(customGameService.getGameDetail(1L).getItems()).isEmpty();
        assertThatThrownBy(() -> customGameService.getGameDetail(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    void getPlayData_returnsPlayItemsForAllTypes() {
        CustomGame game = customGame(1L, "Test Game", List.of(
                new CustomGameItem(null, DataPointType.PHOTO, 10L, 0),
                new CustomGameItem(null, DataPointType.PLACE, 20L, 1),
                new CustomGameItem(null, DataPointType.MOVIE, 30L, 2),
                new CustomGameItem(null, DataPointType.QUESTION, 40L, 3)));
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game));
        when(photoRepo.findById(10L)).thenReturn(Optional.of(photo(10L, "Sami")));
        when(placeRepo.findById(20L)).thenReturn(Optional.of(place(20L, "Home")));
        when(movieRepo.findById(30L)).thenReturn(Optional.of(movie(30L, "Interstellar", "Cooper")));
        when(questionRepo.findById(40L)).thenReturn(Optional.of(question(40L, "Pet?", "Milo")));
        when(patientContextService.getOptionCount(PATIENT_ID)).thenReturn(4);
        when(patientContextService.getGameComplexity(PATIENT_ID)).thenReturn("easy");

        UnifiedPlayData result = customGameService.getPlayData(1L);

        assertThat(result.getGameId()).isEqualTo(1L);
        assertThat(result.getTotalQuestions()).isEqualTo(4);
        assertThat(result.getOptionCount()).isEqualTo(4);
        assertThat(result.getGameComplexity()).isEqualTo("easy");
        assertThat(result.getItems()).extracting(UnifiedPlayData.UnifiedPlayItem::getType)
                .containsExactlyInAnyOrder(DataPointType.PHOTO, DataPointType.PLACE, DataPointType.MOVIE, DataPointType.QUESTION);
    }

    @Test
    void getPlayData_gameNotFound_throwsException() {
        when(gameRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customGameService.getPlayData(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Game not found");
    }

    @Test
    void getRandomPlayData_prioritizesNewWrongThenCorrectItemsAndSkipsMissingItems() {
        PhotoMemory photo = photo(1L, "Photo");
        PlaceMemory place = place(2L, "Place");
        MovieMemory movie = movie(3L, "Movie", "Hero");
        QuestionMemory question = question(4L, "Q?", "A");
        DataPointPerformance wrongPlace = new DataPointPerformance(PATIENT_ID, DataPointType.PLACE, 2L, false);
        DataPointPerformance correctMovie = new DataPointPerformance(PATIENT_ID, DataPointType.MOVIE, 3L, true);

        when(photoRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(photo));
        when(placeRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(place));
        when(movieRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(movie));
        when(questionRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(question));
        when(perfRepo.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(wrongPlace, correctMovie));
        when(photoRepo.findById(1L)).thenReturn(Optional.of(photo));
        when(placeRepo.findById(2L)).thenReturn(Optional.of(place));
        when(movieRepo.findById(3L)).thenReturn(Optional.of(movie));
        when(questionRepo.findById(4L)).thenReturn(Optional.empty());

        UnifiedPlayData result = customGameService.getRandomPlayData(PATIENT_ID, 10);

        assertThat(result.getTitle()).isEqualTo("Random Memory Mix");
        assertThat(result.getItems()).extracting(UnifiedPlayData.UnifiedPlayItem::getType)
                .containsExactlyInAnyOrder(DataPointType.PHOTO, DataPointType.PLACE, DataPointType.MOVIE);
        assertThat(result.getTotalQuestions()).isEqualTo(3);
    }

    @Test
    void getRandomPlayData_respectsLimit() {
        QuestionMemory q1 = question(1L, "Q1?", "A1");
        QuestionMemory q2 = question(2L, "Q2?", "A2");
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
    void submitResults_savesAttemptValidatesAnswersAndUpdatesExistingPerformance() {
        UnifiedSubmitRequest req = new UnifiedSubmitRequest();
        req.setGameId(1L);
        req.setTotalQuestions(2);
        req.setDurationSeconds(30);
        req.setAnswers(List.of(
                new UnifiedSubmitRequest.AnswerEntry(DataPointType.PHOTO, 1L, "Sami", true),
                new UnifiedSubmitRequest.AnswerEntry(DataPointType.QUESTION, 4L, null, false)));

        CustomGame game = customGame(1L, "Game", List.of());
        CustomGameAttempt savedAttempt = new CustomGameAttempt();
        savedAttempt.setId(100L);
        savedAttempt.setScore(1);
        savedAttempt.setTotalQuestions(2);
        savedAttempt.setCompletedAt(LocalDateTime.now());
        DataPointPerformance existing = new DataPointPerformance(PATIENT_ID, DataPointType.PHOTO, 1L, false);
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game));
        when(attemptRepo.save(any(CustomGameAttempt.class))).thenReturn(savedAttempt);
        when(photoRepo.findById(1L)).thenReturn(Optional.of(photo(1L, "Sami")));
        when(questionRepo.findById(4L)).thenReturn(Optional.of(question(4L, "Pet?", "Milo")));
        when(perfRepo.findByPatientKeycloakIdAndDataTypeAndDataPointId(PATIENT_ID, DataPointType.PHOTO, 1L)).thenReturn(Optional.of(existing));
        when(perfRepo.findByPatientKeycloakIdAndDataTypeAndDataPointId(PATIENT_ID, DataPointType.QUESTION, 4L)).thenReturn(Optional.empty());

        UnifiedPlayResult result = customGameService.submitResults(PATIENT_ID, req);

        assertThat(result.getAttemptId()).isEqualTo(100L);
        assertThat(result.getScore()).isEqualTo(1);
        assertThat(result.getPercentage()).isEqualTo(50.0);
        assertThat(result.getResults()).hasSize(2);
        assertThat(result.getResults().get(1).getSelectedAnswer()).isEqualTo("I don't know");
        assertThat(existing.isLastCorrect()).isTrue();
        assertThat(existing.getCorrectCount()).isEqualTo(1);
        verify(perfRepo).save(existing);
        verify(perfRepo).save(argThat(perf -> perf.getDataType() == DataPointType.QUESTION && !perf.isLastCorrect()));
    }

    @Test
    void submitResults_withNullGameIdAndNullAnswers_worksForRandomGames() {
        UnifiedSubmitRequest req = new UnifiedSubmitRequest();
        req.setGameId(null);
        req.setTotalQuestions(0);
        req.setDurationSeconds(10);
        req.setAnswers(null);

        CustomGameAttempt savedAttempt = new CustomGameAttempt();
        savedAttempt.setId(200L);
        savedAttempt.setScore(0);
        savedAttempt.setTotalQuestions(0);
        savedAttempt.setCompletedAt(LocalDateTime.now());
        when(attemptRepo.save(any(CustomGameAttempt.class))).thenReturn(savedAttempt);

        UnifiedPlayResult result = customGameService.submitResults(PATIENT_ID, req);

        assertThat(result.getAttemptId()).isEqualTo(200L);
        assertThat(result.getPercentage()).isZero();
        verify(gameRepo, never()).findById(anyLong());
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

    private CustomGame customGame(Long id, String title, List<CustomGameItem> items) {
        CustomGame game = new CustomGame();
        game.setId(id);
        game.setPatientKeycloakId(PATIENT_ID);
        game.setTitle(title);
        game.setDescription("Description");
        game.setCreatedAt(LocalDateTime.now());
        game.setItems(new ArrayList<>(items));
        game.getItems().forEach(item -> item.setCustomGame(game));
        return game;
    }

    private PhotoMemory photo(Long id, String name) {
        PhotoMemory photo = new PhotoMemory();
        photo.setId(id);
        photo.setPatientKeycloakId(PATIENT_ID);
        photo.setName(name);
        photo.setImageData(new byte[] { 1, 2, 3 });
        photo.setImageContentType("image/png");
        photo.setCreatedAt(LocalDateTime.now());
        return photo;
    }

    private PlaceMemory place(Long id, String name) {
        PlaceMemory place = new PlaceMemory();
        place.setId(id);
        place.setPatientKeycloakId(PATIENT_ID);
        place.setName(name);
        place.setHint("Near the door");
        place.setLatitude(36.8);
        place.setLongitude(10.1);
        place.setCreatedAt(LocalDateTime.now());
        return place;
    }

    private MovieMemory movie(Long id, String title, String answer) {
        MovieMemory movie = new MovieMemory();
        movie.setId(id);
        movie.setPatientKeycloakId(PATIENT_ID);
        movie.setOriginalTitle(title);
        movie.setCorrectAnswer(answer);
        movie.setPosterPath("/poster.jpg");
        movie.setCreatedAt(LocalDateTime.now());
        return movie;
    }

    private QuestionMemory question(Long id, String text, String answer) {
        QuestionMemory question = new QuestionMemory();
        question.setId(id);
        question.setPatientKeycloakId(PATIENT_ID);
        question.setQuestionText(text);
        question.setCorrectAnswer(answer);
        question.setCreatedAt(LocalDateTime.now());
        return question;
    }
}
