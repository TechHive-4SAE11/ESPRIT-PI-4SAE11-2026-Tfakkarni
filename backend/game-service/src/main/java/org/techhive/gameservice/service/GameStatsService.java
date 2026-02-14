package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.gameservice.dto.GameStatsResponse;
import org.techhive.gameservice.dto.OverviewStatsResponse;
import org.techhive.gameservice.repository.GameAttemptRepository;
import org.techhive.gameservice.repository.MiniGameRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameStatsService {

    private final MiniGameRepository miniGameRepository;
    private final GameAttemptRepository gameAttemptRepository;

    /**
     * Get stats for a specific patient/player.
     */
    public GameStatsResponse getPlayerStats(String keycloakId) {
        GameStatsResponse stats = new GameStatsResponse();
        stats.setPlayerKeycloakId(keycloakId);
        stats.setTotalGamesCreated((int) miniGameRepository.countByPatientKeycloakId(keycloakId));
        stats.setTotalGamesPlayed((int) gameAttemptRepository.countDistinctGamesPlayedByPlayer(keycloakId));
        stats.setTotalAttempts((int) gameAttemptRepository.countByPlayerKeycloakId(keycloakId));

        if (stats.getTotalAttempts() > 0) {
            stats.setAverageScore(gameAttemptRepository.averageScorePercentageByPlayer(keycloakId));
            stats.setBestScore(gameAttemptRepository.bestScoreByPlayer(keycloakId));
        }

        return stats;
    }

    /**
     * Get platform-wide stats (admin view).
     */
    public OverviewStatsResponse getOverviewStats() {
        OverviewStatsResponse stats = new OverviewStatsResponse();
        stats.setTotalGames(miniGameRepository.count());
        stats.setTotalAttempts(gameAttemptRepository.count());
        stats.setTotalPlayers(gameAttemptRepository.countDistinctPlayers());

        if (stats.getTotalAttempts() > 0) {
            stats.setAverageScorePercentage(gameAttemptRepository.overallAverageScorePercentage());
        }

        return stats;
    }
}
