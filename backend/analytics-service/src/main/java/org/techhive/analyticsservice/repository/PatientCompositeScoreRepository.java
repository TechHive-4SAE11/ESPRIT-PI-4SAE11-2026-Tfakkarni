package org.techhive.analyticsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.analyticsservice.entity.PatientCompositeScore;

import java.util.Optional;

public interface PatientCompositeScoreRepository extends JpaRepository<PatientCompositeScore, Long> {
    Optional<PatientCompositeScore> findByPatientKeycloakId(String patientKeycloakId);
}
