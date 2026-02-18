package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.MedicationIntakeLog;

// No longer used - kept to avoid breaking existing references during migration.
@Deprecated
public interface MedicationIntakeEntryRepository extends JpaRepository<MedicationIntakeLog, Long> {
}
