package org.techhive.gameservice.repository;

import org.techhive.gameservice.entity.PlaceMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceMemoryRepository extends JpaRepository<PlaceMemory, Long> {
  List<PlaceMemory> findByPatientKeycloakId(String patientKeycloakId);

  @Query("SELECT p FROM PlaceMemory p JOIN p.tags t WHERE p.patientKeycloakId = :kid AND t.id IN :tagIds")
  List<PlaceMemory> findByPatientKeycloakIdAndTagIds(@Param("kid") String patientKeycloakId,
      @Param("tagIds") List<Long> tagIds);

  long countByPatientKeycloakId(String patientKeycloakId);

  @Query("SELECT p.name FROM PlaceMemory p WHERE p.patientKeycloakId = :kid AND p.id <> :excludeId")
  List<String> findOtherNames(@Param("kid") String patientKeycloakId, @Param("excludeId") Long excludeId);
}
