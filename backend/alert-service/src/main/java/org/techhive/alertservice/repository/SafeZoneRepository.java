package org.techhive.alertservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.alertservice.entity.SafeZone;

import java.util.List;

public interface SafeZoneRepository extends JpaRepository<SafeZone, Long> {

  List<SafeZone> findByPatientId(String patientId);

  List<SafeZone> findByPatientIdAndActiveTrue(String patientId);
}
