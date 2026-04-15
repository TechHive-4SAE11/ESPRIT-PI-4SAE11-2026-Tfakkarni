package org.techhive.gameservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.GameStatsResponse;
import org.techhive.gameservice.dto.OverviewStatsResponse;
import org.techhive.gameservice.dto.ScoreAnalyticsResponse;
import org.techhive.gameservice.entity.*;
import org.techhive.gameservice.repository.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameStatsServiceTest {

    @Mock
    private MiniGameRepository miniGameRepository;
    @Mock
    private CustomGameRepository customGameRepository;
    @Mock
    private MovieGameRepository movieGameRepository;
    @Mock
    private PersonalQuestionGameRepository personalQuestionGameRepository;

    @Mock
    private GameAttemptRepository gameAttemptRepository;
    @Mock
    private CustomGameAttemptRepository customGameAttemptRepository;
    @Mock
    private MovieGameAttemptRepository movieGameAttemptRepository;
    @Mock
    private PersonalQuestionAttemptRepository personalQuestionAttemptRepository;

    @InjectMocks
    private GameStatsService gameStatsService;

    private static final String PLAYER_ID = "player-123";

    @Test
    void getPlayerStats_aggregatesAllGameTypes() {
        // Game counts
        when(miniGameRepository.countByPatientKeycloakId(PLAYER_ID)).thenReturn(2L);
        when(customGameRepository.countByPatientKeycloakId(PLAYER_ID)).thenReturn(3L);
        when(movieGameRepository.countByPatientKeycloakId(PLAYER_ID)).thenReturn(1L);
        when(personalQuestionGameRepository.countByPatientKeycloakId(PLAYER_ID)).thenReturn(1L);

        // Attempt counts
        when(gameAttemptRepository.countByPlayerKeycloakId(PLAYER_ID)).thenReturn(5L);
        when(customGameAttemptRepository.countByPlayerKeycloakId(PLAYER_ID)).thenReturn(3L);
        when(movieGameAttemptRepository.countByPlayerKeycloakId(PLAYER_ID)).thenReturn(2L);
        when(personalQuestionAttemptRepository.countByPlayerKeycloakId(PLAYER_ID)).thenReturn(4L);

        // Average scores
        when(gameAttemptRepository.averageScorePercentageByPlayer(PLAYER_ID)).thenReturn(80.0);
        when(customGameAttemptRepository.getAverageScorePercentage(PLAYER_ID)).thenReturn(70.0);
        when(movieGameAttemptRepository.averageScorePercentageByPlayer(PLAYER_ID)).thenReturn(90.0);
        when(personalQuestionAttemptRepository.averageScorePercentageByPlayer(PLAYER_ID)).thenReturn(60.0);

        // Best scores
        when(gameAttemptRepository.bestScoreByPlayer(PLAYER_ID)).thenReturn(95);
        when(customGameAttemptRepository.getBestScore(PLAYER_ID)).thenReturn(85);
        when(movieGameAttemptRepository.bestScoreByPlayer(PLAYER_ID)).thenReturn(100);
        when(personalQuestionAttemptRepository.bestScoreByPlayer(PLAYER_ID)).thenReturn(80);

        GameStatsResponse result = gameStatsService.getPlayerStats(PLAYER_ID);

        assertThat(result.getPlayerKeycloakId()).isEqualTo(PLAYER_ID);
        assertThat(result.getTotalGamesCreated()).isEqualTo(7); // 2+3+1+1
        assertThat(result.getTotalAttempts()).isEqualTo(14); // 5+3+2+4
        assertThat(result.getTotalGamesPlayed()).isEqualTo(14);
        assertThat(result.getBestScore()).isEqualTo(100); // max across all types
        assertThat(result.getAverageScore()).isGreaterThan(0);
    }

    @Test
    void getPlayerStats_noData_returnsZeros() {
        when(miniGameRepository.countByPatientKeycloakId(PLAYER_ID)).thenReturn(0L);
        when(customGameRepository.countByPatientKeycloakId(PLAYER_ID)).thenReturn(0L);
        when(movieGameRepository.countByPatientKeycloakId(PLAYER_ID)).thenReturn(0L);
        when(personalQuestionGameRepository.countByPatientKeycloakId(PLAYER_ID)).thenReturn(0L);

        when(gameAttemptRepository.countByPlayerKeycloakId(PLAYER_ID)).thenReturn(0L);
        when(customGameAttemptRepository.countByPlayerKeycloakId(PLAYER_ID)).thenReturn(0L);
        when(movieGameAttemptRepository.countByPlayerKeycloakId(PLAYER_ID)).thenReturn(0L);
        when(personalQuestionAttemptRepository.countByPlayerKeycloakId(PLAYER_ID)).thenReturn(0L);

        GameStatsResponse result = gameStatsService.getPlayerStats(PLAYER_ID);

        assertThat(result.getTotalGamesCreated()).isZero();
        assertThat(result.getTotalAttempts()).isZero();
        assertThat(result.getTotalGamesPlayed()).isZero();
        assertThat(result.getAverageScore()).isZero();
        assertThat(result.getBestScore()).isZero();
    }

    @Test
    void getOverviewStats_aggregatesPlatformWide() {
        when(miniGameRepository.count()).thenReturn(10L);
        when(customGameRepository.count()).thenReturn(5L);
        when(movieGameRepository.count()).thenReturn(3L);
        when(personalQuestionGameRepository.count()).thenReturn(2L);

        when(gameAttemptRepository.count()).thenReturn(20L);
        when(customGameAttemptRepository.count()).thenReturn(15L);
        when(movieGameAttemptRepository.count()).thenReturn(8L);
        when(personalQuestionAttemptRepository.count()).thenReturn(7L);

        when(gameAttemptRepository.countDistinctPlayers()).thenReturn(5L);
        when(gameAttemptRepository.overallAverageScorePercentage()).thenReturn(72.5);

        OverviewStatsResponse result = gameStatsService.getOverviewStats();

        assertThat(result.getTotalGames()).isEqualTo(20); // 10+5+3+2
        assertThat(result.getTotalAttempts()).isEqualTo(50); // 20+15+8+7
        assertThat(result.getTotalPlayers()).isEqualTo(5);
        assertThat(result.getAverageScorePercentage()).isEqualTo(72.5);
    }

    @Test
    void getOverviewStats_noAttempts_zeroAverage() {
        when(miniGameRepository.count()).thenReturn(0L);
        when(customGameRepository.count()).thenReturn(0L);
        when(movieGameRepository.count()).thenReturn(0L);
        when(personalQuestionGameRepository.count()).thenReturn(0L);

        when(gameAttemptRepository.count()).thenReturn(0L);
        when(customGameAttemptRepository.count()).thenReturn(0L);
        when(movieGameAttemptRepository.count()).thenReturn(0L);
        when(personalQuestionAttemptRepository.count()).thenReturn(0L);

        when(gameAttemptRepository.countDistinctPlayers()).thenReturn(0L);

        OverviewStatsResponse result = gameStatsService.getOverviewStats();

        assertThat(result.getTotalGames()).isZero();
        assertThat(result.getTotalAttempts()).isZero();
        assertThat(result.getAverageScorePercentage()).isZero();
    }

    @Test
    void getScoreAnalytics_returnsChronologicalAttempts() {
        LocalDateTime earlier = LocalDateTime.of(2026, 4, 10, 14, 0);
        LocalDateTime later = LocalDateTime.of(2026, 4, 12, 10, 0);

        // MiniGame attempt
        MiniGame miniGame = new MiniGame(PLAYER_ID, "Image Game", "desc");
        miniGame.setId(1L);
        GameAttempt miniAttempt = new GameAttempt(miniGame, PLAYER_ID, 8, 10, 30);
        miniAttempt.setId(1L);
        miniAttempt.setCompletedAt(later);

        // CustomGame attempt
        CustomGame customGame = new CustomGame();
        customGame.setId(2L);
        customGame.setTitle("Memory Mix");
        CustomGameAttempt customAttempt = new CustomGameAttempt();
        customAttempt.setId(2L);
        customAttempt.setCustomGame(customGame);
        customAttempt.setPlayerKeycloakId(PLAYER_ID);
        customAttempt.setScore(5);
        customAttempt.setTotalQuestions(10);
        customAttempt.setDurationSeconds(45);
        customAttempt.setCompletedAt(earlier);

        when(gameAttemptRepository.findByPlayerKeycloakId(PLAYER_ID)).thenReturn(List.of(miniAttempt));
        when(customGameAttemptRepository.findByPlayerKeycloakIdOrderByCompletedAtDesc(PLAYER_ID))
                .thenReturn(List.of(customAttempt));
        when(movieGameAttemptRepository.findByPlayerKeycloakId(PLAYER_ID)).thenReturn(List.of());
        when(personalQuestionAttemptRepository.findByPlayerKeycloakId(PLAYER_ID)).thenReturn(List.of());

        ScoreAnalyticsResponse result = gameStatsService.getScoreAnalytics(PLAYER_ID);

        assertThat(result.getPatientKeycloakId()).isEqualTo(PLAYER_ID);
        assertThat(result.getTotalGamesPlayed()).isEqualTo(2);
        assertThat(result.getScoreHistory()).hasSize(2);

        // Should be sorted chronologically (earliest first)
        assertThat(result.getScoreHistory().get(0).getCompletedAt()).isBefore(
                result.getScoreHistory().get(1).getCompletedAt());
        assertThat(result.getScoreHistory().get(0).getGameType()).isEqualTo("CUSTOM");
        assertThat(result.getScoreHistory().get(1).getGameType()).isEqualTo("MINI");
    }

    @Test
    void getScoreAnalytics_computesPercentagesCorrectly() {
        MiniGame miniGame = new MiniGame(PLAYER_ID, "Test", "desc");
        miniGame.setId(1L);
        GameAttempt attempt = new GameAttempt(miniGame, PLAYER_ID, 7, 10, 20);
        attempt.setId(1L);
        attempt.setCompletedAt(LocalDateTime.now());

        when(gameAttemptRepository.findByPlayerKeycloakId(PLAYER_ID)).thenReturn(List.of(attempt));
        when(customGameAttemptRepository.findByPlayerKeycloakIdOrderByCompletedAtDesc(PLAYER_ID))
                .thenReturn(List.of());
        when(movieGameAttemptRepository.findByPlayerKeycloakId(PLAYER_ID)).thenReturn(List.of());
        when(personalQuestionAttemptRepository.findByPlayerKeycloakId(PLAYER_ID)).thenReturn(List.of());

        ScoreAnalyticsResponse result = gameStatsService.getScoreAnalytics(PLAYER_ID);

        assertThat(result.getScoreHistory().get(0).getPercentage()).isEqualTo(70.0);
        assertThat(result.getAverageScore()).isEqualTo(70.0);
        assertThat(result.getBestScore()).isEqualTo(7);
    }

    @Test
    void getScoreAnalytics_noAttempts_returnsEmpty() {
        when(gameAttemptRepository.findByPlayerKeycloakId(PLAYER_ID)).thenReturn(List.of());
        when(customGameAttemptRepository.findByPlayerKeycloakIdOrderByCompletedAtDesc(PLAYER_ID))
                .thenReturn(List.of());
        when(movieGameAttemptRepository.findByPlayerKeycloakId(PLAYER_ID)).thenReturn(List.of());
        when(personalQuestionAttemptRepository.findByPlayerKeycloakId(PLAYER_ID)).thenReturn(List.of());

        ScoreAnalyticsResponse result = gameStatsService.getScoreAnalytics(PLAYER_ID);

        assertThat(result.getTotalGamesPlayed()).isZero();
        assertThat(result.getScoreHistory()).isEmpty();
        assertThat(result.getAverageScore()).isZero();
    }
}
