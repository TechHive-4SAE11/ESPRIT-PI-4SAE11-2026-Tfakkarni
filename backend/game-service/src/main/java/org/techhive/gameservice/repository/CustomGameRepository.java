package org.techhive.gameservice.repository;

import org.techhive.gameservice.entity.CustomGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomGameRepository extends JpaRepository<CustomGame, Long> {
  List<CustomGame> findByPatientKeycloakId(String patientKeycloakId);

  long countByPatientKeycloakId(String patientKeycloakId);
}
