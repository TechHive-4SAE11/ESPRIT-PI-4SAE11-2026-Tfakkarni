package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.DailyLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {
    Optional<DailyLog> findByPatientKeycloakIdAndLogDate(String patientKeycloakId, LocalDate logDate);
    List<DailyLog> findByPatientKeycloakIdOrderByLogDateDesc(String patientKeycloakId);
    List<DailyLog> findByPatientKeycloakIdAndLogDateBetweenOrderByLogDateAsc(
            String patientKeycloakId, LocalDate start, LocalDate end);
}