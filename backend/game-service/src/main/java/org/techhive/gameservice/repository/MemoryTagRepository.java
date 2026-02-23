package org.techhive.gameservice.repository;

import org.techhive.gameservice.entity.MemoryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemoryTagRepository extends JpaRepository<MemoryTag, Long> {
  List<MemoryTag> findByPatientKeycloakId(String patientKeycloakId);

  List<MemoryTag> findByPatientKeycloakIdAndNameContainingIgnoreCase(String patientKeycloakId, String name);

  boolean existsByPatientKeycloakIdAndNameIgnoreCase(String patientKeycloakId, String name);
}
