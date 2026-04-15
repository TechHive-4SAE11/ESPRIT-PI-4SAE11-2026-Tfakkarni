package org.techhive.gameservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.PersonalQuestionAttempt;
import org.techhive.gameservice.entity.PersonalQuestionGame;
import org.techhive.gameservice.entity.PersonalQuestionItem;
import org.techhive.gameservice.repository.PersonalQuestionAttemptRepository;
import org.techhive.gameservice.repository.PersonalQuestionGameRepository;
import org.techhive.gameservice.repository.PersonalQuestionItemRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalQuestionServiceTest {

    @Mock
    private PersonalQuestionGameRepository gameRepository;
    @Mock
    private PersonalQuestionItemRepository itemRepository;
    @Mock
    private PersonalQuestionAttemptRepository attemptRepository;

    @InjectMocks
    private PersonalQuestionService personalQuestionService;

    private static final String PATIENT_ID = "patient-abc";

    @Test
    void createGame_savesGameAndQuestions() {
        CreatePersonalQuestionGameRequest req = new CreatePersonalQuestionGameRequest();
        req.setTitle("Personal Quiz");
        req.setDescription("Family questions");

        CreatePersonalQuestionGameRequest.QuestionItemRequest q1 =
                new CreatePersonalQuestionGameRequest.QuestionItemRequest();
        q1.setQuestionText("What is your dog's name?");
        q1.setCorrectAnswer("Rex");

        req.setQuestions(List.of(q1));

        PersonalQuestionGame saved = new PersonalQuestionGame(PATIENT_ID, "Personal Quiz", "Family questions");
        saved.setId(1L);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setItems(new ArrayList<>());

        when(gameRepository.save(any(PersonalQuestionGame.class))).thenReturn(saved);

        PersonalQuestionGameResponse result = personalQuestionService.createGame(PATIENT_ID, req);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Personal Quiz");
        verify(gameRepository, times(2)).save(any(PersonalQuestionGame.class));
    }

    @Test
    void getGamesByPatient_returnsMapped() {
        PersonalQuestionGame g1 = new PersonalQuestionGame(PATIENT_ID, "Quiz 1", "desc");
        g1.setId(1L);
        g1.setCreatedAt(LocalDateTime.now());
        g1.setItems(new ArrayList<>());

        when(gameRepository.findByPatientKeycloakId(PATIENT_ID)).thenReturn(List.of(g1));

        List<PersonalQuestionGameResponse> result = personalQuestionService.getGamesByPatient(PATIENT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Quiz 1");
    }

    @Test
    void getGameForPlay_returnsPlayData() {
        PersonalQuestionGame game = new PersonalQuestionGame(PATIENT_ID, "Quiz", "desc");
        game.setId(1L);

        PersonalQuestionItem item = new PersonalQuestionItem(game, "Favorite color?", "Blue", 0);
        item.setId(10L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(itemRepository.findByGameId(1L)).thenReturn(List.of(item));

        PersonalQuestionPlayData result = personalQuestionService.getGameForPlay(1L);

        assertThat(result.getGameId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Quiz");
        assertThat(result.getTotalQuestions()).isEqualTo(1);
        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().get(0).getQuestionText()).isEqualTo("Favorite color?");
        assertThat(result.getQuestions().get(0).getCorrectAnswer()).isEqualTo("Blue");
    }

    @Test
    void getGameForPlay_notFound_throwsException() {
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personalQuestionService.getGameForPlay(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Personal question game not found");
    }

    @Test
    void getGameForPlay_noQuestions_throwsException() {
        PersonalQuestionGame game = new PersonalQuestionGame(PATIENT_ID, "Empty", "desc");
        game.setId(1L);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(itemRepository.findByGameId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> personalQuestionService.getGameForPlay(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no questions");
    }

    @Test
    void submitResults_savesSelfAssessedScore() {
        PersonalQuestionGame game = new PersonalQuestionGame(PATIENT_ID, "Quiz", "desc");
        game.setId(1L);

        PersonalQuestionSubmitRequest req = new PersonalQuestionSubmitRequest();
        req.setScore(3);
        req.setTotalQuestions(5);
        req.setDurationSeconds(60);

        PersonalQuestionAttempt savedAttempt = new PersonalQuestionAttempt(game, PATIENT_ID, 3, 5, 60);
        savedAttempt.setId(100L);
        savedAttempt.setCompletedAt(LocalDateTime.now());

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(attemptRepository.save(any(PersonalQuestionAttempt.class))).thenReturn(savedAttempt);

        PersonalQuestionAttemptResponse result = personalQuestionService.submitResults(1L, PATIENT_ID, req);

        assertThat(result.getAttemptId()).isEqualTo(100L);
        assertThat(result.getScore()).isEqualTo(3);
        assertThat(result.getTotalQuestions()).isEqualTo(5);
        assertThat(result.getPercentage()).isEqualTo(60.0);
        assertThat(result.getDurationSeconds()).isEqualTo(60);
        verify(attemptRepository).save(any(PersonalQuestionAttempt.class));
    }

    @Test
    void submitResults_zeroQuestions_zeroPercentage() {
        PersonalQuestionGame game = new PersonalQuestionGame(PATIENT_ID, "Quiz", "desc");
        game.setId(1L);

        PersonalQuestionSubmitRequest req = new PersonalQuestionSubmitRequest();
        req.setScore(0);
        req.setTotalQuestions(0);
        req.setDurationSeconds(5);

        PersonalQuestionAttempt savedAttempt = new PersonalQuestionAttempt(game, PATIENT_ID, 0, 0, 5);
        savedAttempt.setId(101L);
        savedAttempt.setCompletedAt(LocalDateTime.now());

        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(attemptRepository.save(any())).thenReturn(savedAttempt);

        PersonalQuestionAttemptResponse result = personalQuestionService.submitResults(1L, PATIENT_ID, req);

        assertThat(result.getPercentage()).isEqualTo(0.0);
    }

    @Test
    void deleteGame_success() {
        when(gameRepository.existsById(1L)).thenReturn(true);

        personalQuestionService.deleteGame(1L);

        verify(gameRepository).deleteById(1L);
    }

    @Test
    void deleteGame_notFound_throwsException() {
        when(gameRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> personalQuestionService.deleteGame(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Personal question game not found");
    }
}
