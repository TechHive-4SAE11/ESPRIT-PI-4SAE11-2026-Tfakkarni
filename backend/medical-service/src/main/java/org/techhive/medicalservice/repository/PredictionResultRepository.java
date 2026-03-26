package org.techhive.medicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.medicalservice.entity.PredictionResult;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionResultRepository extends JpaRepository<PredictionResult, Long> {

    List<PredictionResult> findByRiskScoreGreaterThanEqual(Integer riskScore);

    Optional<PredictionResult> findTopByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);
}
