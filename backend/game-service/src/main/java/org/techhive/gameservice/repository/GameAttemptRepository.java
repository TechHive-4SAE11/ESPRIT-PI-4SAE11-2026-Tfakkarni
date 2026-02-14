package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.gameservice.entity.GameAttempt;

import java.util.List;

public interface GameAttemptRepository extends JpaRepository<GameAttempt, Long> {

    List<GameAttempt> findByPlayerKeycloakId(String playerKeycloakId);

    List<GameAttempt> findByMiniGameId(Long miniGameId);

    List<GameAttempt> findByMiniGameIdAndPlayerKeycloakId(Long miniGameId, String playerKeycloakId);

    long countByPlayerKeycloakId(String playerKeycloakId);

    @Query("SELECT COALESCE(AVG(CAST(a.score AS double) / a.totalQuestions * 100), 0) FROM GameAttempt a WHERE a.playerKeycloakId = :keycloakId")
    double averageScorePercentageByPlayer(@Param("keycloakId") String keycloakId);

    @Query("SELECT COALESCE(MAX(a.score), 0) FROM GameAttempt a WHERE a.playerKeycloakId = :keycloakId")
    int bestScoreByPlayer(@Param("keycloakId") String keycloakId);

    @Query("SELECT COUNT(DISTINCT a.miniGame.id) FROM GameAttempt a WHERE a.playerKeycloakId = :keycloakId")
    long countDistinctGamesPlayedByPlayer(@Param("keycloakId") String keycloakId);

    @Query("SELECT COUNT(DISTINCT a.playerKeycloakId) FROM GameAttempt a")
    long countDistinctPlayers();

    @Query("SELECT COALESCE(AVG(CAST(a.score AS double) / a.totalQuestions * 100), 0) FROM GameAttempt a")
    double overallAverageScorePercentage();
}
