package org.techhive.analyticsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.analyticsservice.entity.FeatureGate;

import java.util.Optional;

public interface FeatureGateRepository extends JpaRepository<FeatureGate, Long> {
    Optional<FeatureGate> findByPatientKeycloakId(String patientKeycloakId);
}
