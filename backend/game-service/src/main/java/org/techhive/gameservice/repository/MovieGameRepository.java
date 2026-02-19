package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.gameservice.entity.MovieGame;

import java.util.List;

public interface MovieGameRepository extends JpaRepository<MovieGame, Long> {

  List<MovieGame> findByPatientKeycloakId(String patientKeycloakId);

  long countByPatientKeycloakId(String patientKeycloakId);
}
