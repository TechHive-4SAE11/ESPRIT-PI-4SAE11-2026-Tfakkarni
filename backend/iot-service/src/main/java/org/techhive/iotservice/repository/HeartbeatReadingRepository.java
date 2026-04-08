package org.techhive.iotservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.techhive.iotservice.entity.HeartbeatReading;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HeartbeatReadingRepository extends JpaRepository<HeartbeatReading, Long> {

    List<HeartbeatReading> findByPatientIdAndTimestampBetweenOrderByTimestampAsc(
            String patientId, LocalDateTime start, LocalDateTime end);

    Optional<HeartbeatReading> findFirstByPatientIdOrderByTimestampDesc(String patientId);

    long countByPatientId(String patientId);

    @Query("SELECT DISTINCT h.patientId FROM HeartbeatReading h")
    List<String> findDistinctPatientIds();
}
