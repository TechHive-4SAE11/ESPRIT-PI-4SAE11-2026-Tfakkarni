package org.techhive.analyticsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.analyticsservice.entity.ScoreHistory;

import java.time.LocalDateTime;
import java.util.List;

public interface ScoreHistoryRepository extends JpaRepository<ScoreHistory, Long> {
    List<ScoreHistory> findByPatientKeycloakIdAndRecordedAtAfterOrderByRecordedAtAsc(
            String patientKeycloakId, LocalDateTime after);

    List<ScoreHistory> findTop2ByPatientKeycloakIdOrderByRecordedAtDesc(String patientKeycloakId);
}
