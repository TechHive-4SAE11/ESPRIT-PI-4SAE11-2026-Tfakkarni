package org.techhive.analyticsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.analyticsservice.entity.DoctorEffectivenessScore;

import java.util.List;
import java.util.Optional;

public interface DoctorEffectivenessScoreRepository extends JpaRepository<DoctorEffectivenessScore, Long> {
    Optional<DoctorEffectivenessScore> findByDoctorKeycloakId(String doctorKeycloakId);
    List<DoctorEffectivenessScore> findAllByOrderByStabilizationRateDesc();
    List<DoctorEffectivenessScore> findByRiskFlagsIsNotNull();
}
