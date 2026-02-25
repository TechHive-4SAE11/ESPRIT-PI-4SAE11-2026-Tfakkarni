package org.techhive.gameservice.repository;

import org.techhive.gameservice.entity.MovieMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieMemoryRepository extends JpaRepository<MovieMemory, Long> {
  List<MovieMemory> findByPatientKeycloakId(String patientKeycloakId);

  @Query("SELECT m FROM MovieMemory m JOIN m.tags t WHERE m.patientKeycloakId = :kid AND t.id IN :tagIds")
  List<MovieMemory> findByPatientKeycloakIdAndTagIds(@Param("kid") String patientKeycloakId,
      @Param("tagIds") List<Long> tagIds);

  long countByPatientKeycloakId(String patientKeycloakId);

  @Query("SELECT m.correctAnswer FROM MovieMemory m WHERE m.patientKeycloakId = :kid AND m.id <> :excludeId")
  List<String> findOtherAnswers(@Param("kid") String patientKeycloakId, @Param("excludeId") Long excludeId);
}
