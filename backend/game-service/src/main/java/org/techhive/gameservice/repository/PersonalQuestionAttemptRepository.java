package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.techhive.gameservice.entity.PersonalQuestionAttempt;

import java.util.List;

public interface PersonalQuestionAttemptRepository extends JpaRepository<PersonalQuestionAttempt, Long> {

  List<PersonalQuestionAttempt> findByPlayerKeycloakId(String playerKeycloakId);

  List<PersonalQuestionAttempt> findByGameId(Long gameId);

  long countByPlayerKeycloakId(String playerKeycloakId);

  @Query("SELECT COALESCE(AVG(CAST(a.score AS double) / a.totalQuestions * 100), 0) FROM PersonalQuestionAttempt a WHERE a.playerKeycloakId = :keycloakId")
  double averageScorePercentageByPlayer(@Param("keycloakId") String keycloakId);

  @Query("SELECT COALESCE(MAX(a.score), 0) FROM PersonalQuestionAttempt a WHERE a.playerKeycloakId = :keycloakId")
  int bestScoreByPlayer(@Param("keycloakId") String keycloakId);
}
