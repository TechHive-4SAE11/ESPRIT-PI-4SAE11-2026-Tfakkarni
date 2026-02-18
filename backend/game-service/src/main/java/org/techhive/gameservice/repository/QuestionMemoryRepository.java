package org.techhive.gameservice.repository;

import org.techhive.gameservice.entity.QuestionMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionMemoryRepository extends JpaRepository<QuestionMemory, Long> {
  List<QuestionMemory> findByPatientKeycloakId(String patientKeycloakId);

  @Query("SELECT q FROM QuestionMemory q JOIN q.tags t WHERE q.patientKeycloakId = :kid AND t.id IN :tagIds")
  List<QuestionMemory> findByPatientKeycloakIdAndTagIds(@Param("kid") String patientKeycloakId,
      @Param("tagIds") List<Long> tagIds);

  long countByPatientKeycloakId(String patientKeycloakId);
}
