package org.techhive.gameservice.repository;

import org.techhive.gameservice.entity.CustomGameAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomGameAttemptRepository extends JpaRepository<CustomGameAttempt, Long> {
  List<CustomGameAttempt> findByPlayerKeycloakIdOrderByCompletedAtDesc(String playerKeycloakId);

  List<CustomGameAttempt> findByCustomGameIdOrderByCompletedAtDesc(Long customGameId);

  long countByPlayerKeycloakId(String playerKeycloakId);

  @Query("SELECT COALESCE(AVG(a.score * 100.0 / a.totalQuestions), 0) FROM CustomGameAttempt a WHERE a.playerKeycloakId = :kid")
  double getAverageScorePercentage(@Param("kid") String playerKeycloakId);

  @Query("SELECT COALESCE(MAX(a.score), 0) FROM CustomGameAttempt a WHERE a.playerKeycloakId = :kid")
  int getBestScore(@Param("kid") String playerKeycloakId);
}
