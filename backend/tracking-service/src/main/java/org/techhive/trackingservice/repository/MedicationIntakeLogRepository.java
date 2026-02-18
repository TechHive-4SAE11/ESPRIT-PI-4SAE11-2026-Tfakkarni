package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.MedicationIntakeLog;

public interface MedicationIntakeLogRepository extends JpaRepository<MedicationIntakeLog, Long> {
}
