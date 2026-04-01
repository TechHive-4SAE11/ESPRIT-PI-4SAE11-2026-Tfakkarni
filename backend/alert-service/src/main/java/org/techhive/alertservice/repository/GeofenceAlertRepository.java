package org.techhive.alertservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.alertservice.entity.GeofenceAlert;

import java.util.List;

public interface GeofenceAlertRepository extends JpaRepository<GeofenceAlert, Long> {

  List<GeofenceAlert> findByPatientIdOrderByCreatedAtDesc(String patientId);

  List<GeofenceAlert> findByPatientIdAndAcknowledgedFalse(String patientId);
}
