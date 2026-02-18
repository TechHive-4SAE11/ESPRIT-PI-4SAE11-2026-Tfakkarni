package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.gameservice.entity.MovieGameAttempt;

import java.util.List;

public interface MovieGameAttemptRepository extends JpaRepository<MovieGameAttempt, Long> {

  List<MovieGameAttempt> findByPlayerKeycloakId(String playerKeycloakId);

  List<MovieGameAttempt> findByMovieGameId(Long movieGameId);

  long countByPlayerKeycloakId(String playerKeycloakId);

  @Query("SELECT COALESCE(AVG(CAST(a.score AS double) / a.totalQuestions * 100), 0) FROM MovieGameAttempt a WHERE a.playerKeycloakId = :keycloakId")
  double averageScorePercentageByPlayer(@Param("keycloakId") String keycloakId);

  @Query("SELECT COALESCE(MAX(a.score), 0) FROM MovieGameAttempt a WHERE a.playerKeycloakId = :keycloakId")
  int bestScoreByPlayer(@Param("keycloakId") String keycloakId);
}
