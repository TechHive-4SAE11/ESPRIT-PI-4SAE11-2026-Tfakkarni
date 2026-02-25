package org.techhive.gameservice.repository;

import org.techhive.gameservice.entity.DataPointPerformance;
import org.techhive.gameservice.entity.DataPointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataPointPerformanceRepository extends JpaRepository<DataPointPerformance, Long> {

  /**
   * Find all performance records for a patient.
   */
  List<DataPointPerformance> findByPatientKeycloakId(String patientKeycloakId);

  /**
   * Find the performance record for a specific patient + data point.
   */
  Optional<DataPointPerformance> findByPatientKeycloakIdAndDataTypeAndDataPointId(
      String patientKeycloakId, DataPointType dataType, Long dataPointId);

  /**
   * Find all data points where the last answer was incorrect.
   */
  List<DataPointPerformance> findByPatientKeycloakIdAndLastCorrectFalse(String patientKeycloakId);

  /**
   * Find all data points where the last answer was correct.
   */
  List<DataPointPerformance> findByPatientKeycloakIdAndLastCorrectTrue(String patientKeycloakId);
}
