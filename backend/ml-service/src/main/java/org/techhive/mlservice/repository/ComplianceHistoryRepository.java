package org.techhive.mlservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.mlservice.entity.ComplianceHistory;
import java.time.LocalDateTime;
import java.util.List;

public interface ComplianceHistoryRepository extends JpaRepository<ComplianceHistory, Long> {

    List<ComplianceHistory> findByPatientIdAndDateAfterOrderByDateAsc(String patientId, LocalDateTime date);
}