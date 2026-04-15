package org.techhive.gameservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.MovieGame;
import org.techhive.gameservice.entity.MovieGameAttempt;
import org.techhive.gameservice.entity.MovieGameItem;
import org.techhive.gameservice.repository.MovieGameAttemptRepository;
import org.techhive.gameservice.repository.MovieGameItemRepository;
import org.techhive.gameservice.repository.MovieGameRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieGameServiceTest {

    @Mock
    private MovieGameRepository movieGameRepository;
    @Mock
    private MovieGameItemRepository movieGameItemRepository;
    @Mock
    private MovieGameAttemptRepository movieGameAttemptRepository;

    @InjectMocks
    private MovieGameService movieGameService;

    private static final String PATIENT_ID = "patient-abc";

    @Test
    void createMovieGame_savesGameAndItems() {
        CreateMovieGameRequest req = new CreateMovieGameRequest();
        req.setTitle("Movie Quiz");
        req.setDescription("Guess the character");

        CreateMovieGameRequest.MovieItemRequest item1 = new CreateMovieGameRequest.MovieItemRequest();
        item1.setTmdbId(550);
        item1.setOriginalTitle("Fight Club");
        item1.setPosterPath("/poster.jpg");
        item1.setReleaseDate("1999-10-15");
        item1.setCorrectAnswer("Tyler Durden");

        CreateMovieGameRequest.MovieItemRequest item2 = new CreateMovieGameRequest.MovieItemRequest();
        item2.setTmdbId(680);
        item2.setOriginalTitle("Pulp Fiction");
        item2.setPosterPath("/pulp.jpg");
        item2.setReleaseDate("1994-10-14");
        item2.setCorrectAnswer("Vincent Vega");

        req.setMovies(List.of(item1, item2));

        MovieGame saved = new MovieGame(PATIENT_ID, "Movie Quiz", "Guess the character");
        saved.setId(1L);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setItems(new ArrayList<>());

        when(movieGameRepository.save(any(MovieGame.class))).thenReturn(saved);

        MovieGameResponse result = movieGameService.createMovieGame(PATIENT_ID, req);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Movie Quiz");
        verify(movieGameRepository, times(2)).save(any(MovieGame.class));
    }

    @Test
    void getGamesByPatient_returnsMapped() {
        MovieGame g1 = new MovieGame(PATIENT_ID, "Game 1", "desc");
        g1.setId(1L);
        g1.setCreatedAt(LocalDateTime.now());
        g1.setItems(new ArrayList<>());

        when(movieGameRepository.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(g1));

        List<MovieGameResponse> result = movieGameService.getGamesByPatient(PATIENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Game 1");
    }

    @Test
    void getGameForPlay_returnsPlayDataWithChoices() {
        MovieGame game = new MovieGame(PATIENT_ID, "Movie Quiz", "desc");
        game.setId(1L);

        MovieGameItem item1 = new MovieGameItem(game, 550, "Fight Club", "/poster1.jpg", "1999", "Tyler Durden", 0);
        item1.setId(10L);
        MovieGameItem item2 = new MovieGameItem(game, 680, "Pulp Fiction", "/poster2.jpg", "1994", "Vincent Vega", 1);
        item2.setId(11L);

        when(movieGameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(movieGameItemRepository.findByMovieGameId(1L)).thenReturn(List.of(item1, item2));

        MovieGamePlayData result = movieGameService.getGameForPlay(1L);

        assertThat(result.getGameId()).isEqualTo(1L);
        assertThat(result.getTotalQuestions()).isEqualTo(2);
        assertThat(result.getQuestions()).hasSize(2);
        // Each question's choices should contain all correct answers
        for (MovieGamePlayData.MovieQuestion q : result.getQuestions()) {
            assertThat(q.getChoices()).contains("Tyler Durden", "Vincent Vega");
        }
    }

    @Test
    void getGameForPlay_notFound_throwsException() {
        when(movieGameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieGameService.getGameForPlay(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Movie game not found");
    }

    @Test
    void getGameForPlay_lessThan2Movies_throwsException() {
        MovieGame game = new MovieGame(PATIENT_ID, "Single", "desc");
        game.setId(1L);

        MovieGameItem item = new MovieGameItem(game, 550, "Fight Club", "/poster.jpg", "1999", "Tyler", 0);
        item.setId(10L);

        when(movieGameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(movieGameItemRepository.findByMovieGameId(1L)).thenReturn(List.of(item));

        assertThatThrownBy(() -> movieGameService.getGameForPlay(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("at least 2 movies");
    }

    @Test
    void submitAnswers_correctAnswers_fullScore() {
        MovieGame game = new MovieGame(PATIENT_ID, "Quiz", "desc");
        game.setId(1L);

        MovieGameItem item1 = new MovieGameItem(game, 550, "Fight Club", "/p1.jpg", "1999", "Tyler Durden", 0);
        item1.setId(10L);
        MovieGameItem item2 = new MovieGameItem(game, 680, "Pulp Fiction", "/p2.jpg", "1994", "Vincent Vega", 1);
        item2.setId(11L);

        when(movieGameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(movieGameItemRepository.findByMovieGameId(1L)).thenReturn(List.of(item1, item2));

        MovieGameSubmitRequest req = new MovieGameSubmitRequest();
        MovieGameSubmitRequest.MovieAnswerEntry a1 = new MovieGameSubmitRequest.MovieAnswerEntry();
        a1.setItemId(10L);
        a1.setSelectedAnswer("Tyler Durden");
        MovieGameSubmitRequest.MovieAnswerEntry a2 = new MovieGameSubmitRequest.MovieAnswerEntry();
        a2.setItemId(11L);
        a2.setSelectedAnswer("Vincent Vega");
        req.setAnswers(List.of(a1, a2));
        req.setDurationSeconds(45);

        MovieGameAttempt savedAttempt = new MovieGameAttempt(game, PATIENT_ID, 2, 2, 45);
        savedAttempt.setId(100L);
        savedAttempt.setCompletedAt(LocalDateTime.now());
        when(movieGameAttemptRepository.save(any(MovieGameAttempt.class))).thenReturn(savedAttempt);

        MovieGameAttemptResponse result = movieGameService.submitAnswers(1L, PATIENT_ID, req);

        assertThat(result.getScore()).isEqualTo(2);
        assertThat(result.getTotalQuestions()).isEqualTo(2);
        assertThat(result.getPercentage()).isEqualTo(100.0);
        assertThat(result.getResults()).hasSize(2);
        assertThat(result.getResults()).allSatisfy(r -> assertThat(r.isCorrect()).isTrue());
    }

    @Test
    void submitAnswers_wrongAnswers_zeroScore() {
        MovieGame game = new MovieGame(PATIENT_ID, "Quiz", "desc");
        game.setId(1L);

        MovieGameItem item1 = new MovieGameItem(game, 550, "Fight Club", "/p1.jpg", "1999", "Tyler Durden", 0);
        item1.setId(10L);

        when(movieGameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(movieGameItemRepository.findByMovieGameId(1L)).thenReturn(List.of(item1));

        MovieGameSubmitRequest req = new MovieGameSubmitRequest();
        MovieGameSubmitRequest.MovieAnswerEntry a1 = new MovieGameSubmitRequest.MovieAnswerEntry();
        a1.setItemId(10L);
        a1.setSelectedAnswer("Wrong Answer");
        req.setAnswers(List.of(a1));
        req.setDurationSeconds(20);

        MovieGameAttempt savedAttempt = new MovieGameAttempt(game, PATIENT_ID, 0, 1, 20);
        savedAttempt.setId(101L);
        savedAttempt.setCompletedAt(LocalDateTime.now());
        when(movieGameAttemptRepository.save(any(MovieGameAttempt.class))).thenReturn(savedAttempt);

        MovieGameAttemptResponse result = movieGameService.submitAnswers(1L, PATIENT_ID, req);

        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.getResults().get(0).isCorrect()).isFalse();
    }

    @Test
    void submitAnswers_caseInsensitiveComparison() {
        MovieGame game = new MovieGame(PATIENT_ID, "Quiz", "desc");
        game.setId(1L);

        MovieGameItem item = new MovieGameItem(game, 550, "Fight Club", "/p.jpg", "1999", "Tyler Durden", 0);
        item.setId(10L);

        when(movieGameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(movieGameItemRepository.findByMovieGameId(1L)).thenReturn(List.of(item));

        MovieGameSubmitRequest req = new MovieGameSubmitRequest();
        MovieGameSubmitRequest.MovieAnswerEntry a = new MovieGameSubmitRequest.MovieAnswerEntry();
        a.setItemId(10L);
        a.setSelectedAnswer("tyler durden"); // lowercase
        req.setAnswers(List.of(a));
        req.setDurationSeconds(10);

        MovieGameAttempt savedAttempt = new MovieGameAttempt(game, PATIENT_ID, 1, 1, 10);
        savedAttempt.setId(102L);
        savedAttempt.setCompletedAt(LocalDateTime.now());
        when(movieGameAttemptRepository.save(any())).thenReturn(savedAttempt);

        MovieGameAttemptResponse result = movieGameService.submitAnswers(1L, PATIENT_ID, req);

        assertThat(result.getResults().get(0).isCorrect()).isTrue();
    }

    @Test
    void deleteGame_success() {
        when(movieGameRepository.existsById(1L)).thenReturn(true);

        movieGameService.deleteGame(1L);

        verify(movieGameRepository).deleteById(1L);
    }

    @Test
    void deleteGame_notFound_throwsException() {
        when(movieGameRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> movieGameService.deleteGame(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Movie game not found");
    }
}
