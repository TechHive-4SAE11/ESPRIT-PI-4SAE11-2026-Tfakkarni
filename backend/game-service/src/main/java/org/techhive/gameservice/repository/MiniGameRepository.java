package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.gameservice.entity.MiniGame;

import java.util.List;

public interface MiniGameRepository extends JpaRepository<MiniGame, Long> {

    List<MiniGame> findByPatientKeycloakId(String patientKeycloakId);

    long countByPatientKeycloakId(String patientKeycloakId);
}
