package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.gameservice.entity.PersonalQuestionGame;

import java.util.List;

public interface PersonalQuestionGameRepository extends JpaRepository<PersonalQuestionGame, Long> {

  List<PersonalQuestionGame> findByPatientKeycloakId(String patientKeycloakId);

  long countByPatientKeycloakId(String patientKeycloakId);
}
