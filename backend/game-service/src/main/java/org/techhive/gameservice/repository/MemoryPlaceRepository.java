package org.techhive.gameservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.techhive.gameservice.entity.MemoryPlace;

import java.util.List;

public interface MemoryPlaceRepository extends JpaRepository<MemoryPlace, Long> {

  List<MemoryPlace> findByPatientKeycloakId(String patientKeycloakId);

  long countByPatientKeycloakId(String patientKeycloakId);

  @Query(value = "SELECT * FROM memory_places WHERE patient_keycloak_id = :patientKeycloakId ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
  List<MemoryPlace> findRandomByPatientKeycloakId(String patientKeycloakId, int limit);
}
