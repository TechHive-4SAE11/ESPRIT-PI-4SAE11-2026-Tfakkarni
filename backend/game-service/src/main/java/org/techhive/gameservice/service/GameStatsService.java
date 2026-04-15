package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.gameservice.dto.GameStatsResponse;
import org.techhive.gameservice.dto.OverviewStatsResponse;
import org.techhive.gameservice.dto.ScoreAnalyticsResponse;
import org.techhive.gameservice.dto.UserResponse;
import org.techhive.gameservice.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameStatsService {

    // Game definition repositories (for counting created games)
    private final MiniGameRepository miniGameRepository;
    private final CustomGameRepository customGameRepository;
    private final MovieGameRepository movieGameRepository;
    private final PersonalQuestionGameRepository personalQuestionGameRepository;

    // Attempt repositories (for counting played games / scores)
    private final GameAttemptRepository gameAttemptRepository;
    private final CustomGameAttemptRepository customGameAttemptRepository;
    private final MovieGameAttemptRepository movieGameAttemptRepository;
    private final PersonalQuestionAttemptRepository personalQuestionAttemptRepository;
    private final PatientContextService patientContextService;

    /**
     * Get stats for a specific patient/player.
     * Aggregates across ALL game types (MiniGame, CustomGame, MovieGame,
     * PersonalQuestion).
     */
    public GameStatsResponse getPlayerStats(String keycloakId) {
        GameStatsResponse stats = new GameStatsResponse();
        stats.setPlayerKeycloakId(keycloakId);

        // Enrich with patient name from user-service
        UserResponse user = patientContextService.getPatientInfo(keycloakId);
        if (user != null) {
            stats.setPlayerName(user.getFirstName() + " " + user.getLastName());
        }

        // Count games created across ALL game types
        long miniGames = miniGameRepository.countByPatientKeycloakId(keycloakId);
        long customGames = customGameRepository.countByPatientKeycloakId(keycloakId);
        long movieGames = movieGameRepository.countByPatientKeycloakId(keycloakId);
        long personalGames = personalQuestionGameRepository.countByPatientKeycloakId(keycloakId);
        stats.setTotalGamesCreated((int) (miniGames + customGames + movieGames + personalGames));

        // Count attempts across ALL game types
        long miniAttempts = gameAttemptRepository.countByPlayerKeycloakId(keycloakId);
        long customAttempts = customGameAttemptRepository.countByPlayerKeycloakId(keycloakId);
        long movieAttempts = movieGameAttemptRepository.countByPlayerKeycloakId(keycloakId);
        long personalAttempts = personalQuestionAttemptRepository.countByPlayerKeycloakId(keycloakId);
        long totalAttempts = miniAttempts + customAttempts + movieAttempts + personalAttempts;

        stats.setTotalAttempts((int) totalAttempts);
        stats.setTotalGamesPlayed((int) totalAttempts); // each attempt = a game played

        if (totalAttempts > 0) {
            // Weighted average score across all game types
            double totalPct = 0;
            long count = 0;
            if (miniAttempts > 0) {
                totalPct += gameAttemptRepository.averageScorePercentageByPlayer(keycloakId) * miniAttempts;
                count += miniAttempts;
                stats.setBestScore(Math.max(stats.getBestScore(), gameAttemptRepository.bestScoreByPlayer(keycloakId)));
            }
            if (customAttempts > 0) {
                totalPct += customGameAttemptRepository.getAverageScorePercentage(keycloakId) * customAttempts;
                count += customAttempts;
                stats.setBestScore(
                        Math.max(stats.getBestScore(), customGameAttemptRepository.getBestScore(keycloakId)));
            }
            if (movieAttempts > 0) {
                totalPct += movieGameAttemptRepository.averageScorePercentageByPlayer(keycloakId) * movieAttempts;
                count += movieAttempts;
                stats.setBestScore(
                        Math.max(stats.getBestScore(), movieGameAttemptRepository.bestScoreByPlayer(keycloakId)));
            }
            if (personalAttempts > 0) {
                totalPct += personalQuestionAttemptRepository.averageScorePercentageByPlayer(keycloakId)
                        * personalAttempts;
                count += personalAttempts;
                stats.setBestScore(Math.max(stats.getBestScore(),
                        personalQuestionAttemptRepository.bestScoreByPlayer(keycloakId)));
            }
            stats.setAverageScore(count > 0 ? totalPct / count : 0);
        }

        return stats;
    }

    /**
     * Get platform-wide stats (admin view).
     */
    public OverviewStatsResponse getOverviewStats() {
        OverviewStatsResponse stats = new OverviewStatsResponse();
        stats.setTotalGames(miniGameRepository.count() + customGameRepository.count()
                + movieGameRepository.count() + personalQuestionGameRepository.count());
        stats.setTotalAttempts(gameAttemptRepository.count() + customGameAttemptRepository.count()
                + movieGameAttemptRepository.count() + personalQuestionAttemptRepository.count());
        stats.setTotalPlayers(gameAttemptRepository.countDistinctPlayers());

        if (stats.getTotalAttempts() > 0) {
            stats.setAverageScorePercentage(gameAttemptRepository.overallAverageScorePercentage());
        }

        return stats;
    }

    /**
     * Get comprehensive score analytics for a patient (doctor view).
     * Aggregates attempts from ALL game types: MiniGame, CustomGame, MovieGame,
     * PersonalQuestion.
     */
    @Transactional(readOnly = true)
    public ScoreAnalyticsResponse getScoreAnalytics(String keycloakId) {
        List<ScoreAnalyticsResponse.AttemptPoint> allPoints = new ArrayList<>();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 1. MiniGame attempts
        var miniAttempts = gameAttemptRepository.findByPlayerKeycloakId(keycloakId);
        for (var a : miniAttempts) {
            double pct = a.getTotalQuestions() > 0 ? (a.getScore() * 100.0 / a.getTotalQuestions()) : 0;
            allPoints.add(ScoreAnalyticsResponse.AttemptPoint.builder()
                    .attemptId(a.getId())
                    .gameType("MINI")
                    .gameTitle(a.getMiniGame() != null ? a.getMiniGame().getTitle() : "Image Game")
                    .score(a.getScore())
                    .totalQuestions(a.getTotalQuestions())
                    .percentage(Math.round(pct * 10.0) / 10.0)
                    .durationSeconds(a.getDurationSeconds())
                    .completedAt(a.getCompletedAt())
                    .build());
        }

        // 2. CustomGame attempts
        var customAttempts = customGameAttemptRepository.findByPlayerKeycloakIdOrderByCompletedAtDesc(keycloakId);
        for (var a : customAttempts) {
            double pct = a.getTotalQuestions() > 0 ? (a.getScore() * 100.0 / a.getTotalQuestions()) : 0;
            allPoints.add(ScoreAnalyticsResponse.AttemptPoint.builder()
                    .attemptId(a.getId())
                    .gameType("CUSTOM")
                    .gameTitle(a.getCustomGame() != null ? a.getCustomGame().getTitle() : "Memory Mix")
                    .score(a.getScore())
                    .totalQuestions(a.getTotalQuestions())
                    .percentage(Math.round(pct * 10.0) / 10.0)
                    .durationSeconds(a.getDurationSeconds())
                    .completedAt(a.getCompletedAt())
                    .build());
        }

        // 3. MovieGame attempts
        var movieAttempts = movieGameAttemptRepository.findByPlayerKeycloakId(keycloakId);
        for (var a : movieAttempts) {
            double pct = a.getTotalQuestions() > 0 ? (a.getScore() * 100.0 / a.getTotalQuestions()) : 0;
            allPoints.add(ScoreAnalyticsResponse.AttemptPoint.builder()
                    .attemptId(a.getId())
                    .gameType("MOVIE")
                    .gameTitle(a.getMovieGame() != null ? a.getMovieGame().getTitle() : "Movie Game")
                    .score(a.getScore())
                    .totalQuestions(a.getTotalQuestions())
                    .percentage(Math.round(pct * 10.0) / 10.0)
                    .durationSeconds(a.getDurationSeconds())
                    .completedAt(a.getCompletedAt())
                    .build());
        }

        // 4. PersonalQuestion attempts
        var personalAttempts = personalQuestionAttemptRepository.findByPlayerKeycloakId(keycloakId);
        for (var a : personalAttempts) {
            double pct = a.getTotalQuestions() > 0 ? (a.getScore() * 100.0 / a.getTotalQuestions()) : 0;
            allPoints.add(ScoreAnalyticsResponse.AttemptPoint.builder()
                    .attemptId(a.getId())
                    .gameType("PERSONAL")
                    .gameTitle(a.getGame() != null ? a.getGame().getTitle() : "Personal Questions")
                    .score(a.getScore())
                    .totalQuestions(a.getTotalQuestions())
                    .percentage(Math.round(pct * 10.0) / 10.0)
                    .durationSeconds(a.getDurationSeconds())
                    .completedAt(a.getCompletedAt())
                    .build());
        }

        // Sort by completedAt ascending (oldest → newest for charting)
        allPoints.sort(Comparator.comparing(ScoreAnalyticsResponse.AttemptPoint::getCompletedAt));

        // Compute summaries
        int totalGames = allPoints.size();
        int gamesLast7Days = (int) allPoints.stream()
                .filter(p -> p.getCompletedAt().isAfter(sevenDaysAgo))
                .count();

        double avgScore = allPoints.stream()
                .mapToDouble(ScoreAnalyticsResponse.AttemptPoint::getPercentage)
                .average().orElse(0);

        double avgScoreLast7 = allPoints.stream()
                .filter(p -> p.getCompletedAt().isAfter(sevenDaysAgo))
                .mapToDouble(ScoreAnalyticsResponse.AttemptPoint::getPercentage)
                .average().orElse(0);

        int bestScore = allPoints.stream()
                .mapToInt(ScoreAnalyticsResponse.AttemptPoint::getScore)
                .max().orElse(0);

        return ScoreAnalyticsResponse.builder()
                .patientKeycloakId(keycloakId)
                .totalGamesPlayed(totalGames)
                .gamesLast7Days(gamesLast7Days)
                .averageScore(Math.round(avgScore * 10.0) / 10.0)
                .averageScoreLast7Days(Math.round(avgScoreLast7 * 10.0) / 10.0)
                .bestScore(bestScore)
                .scoreHistory(allPoints)
                .build();
    }
}
