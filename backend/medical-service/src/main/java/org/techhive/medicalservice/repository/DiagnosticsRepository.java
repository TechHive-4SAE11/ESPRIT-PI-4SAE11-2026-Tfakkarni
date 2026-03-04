package org.techhive.medicalservice.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.techhive.medicalservice.entity.Diagnostics;

import java.time.LocalDateTime;

@Repository
public interface DiagnosticsRepository extends JpaRepository<Diagnostics, Long> {

	List<Diagnostics> findByMedicalFolderId(Long medicalFolderId);

	@Query("SELECT d FROM Diagnostics d JOIN FETCH d.medicalFolder m WHERE LOWER(d.diseaseName) LIKE LOWER(CONCAT('%', :diseaseName, '%')) AND (:stage IS NULL OR :stage = '' OR d.stage = :stage) ORDER BY d.diagnosisDate DESC")
	List<Diagnostics> findByDiseaseNameContainingIgnoreCaseAndOptionalStage(@Param("diseaseName") String diseaseName, @Param("stage") String stage);

	@Query("SELECT d.diseaseName, COUNT(d) FROM Diagnostics d GROUP BY d.diseaseName ORDER BY COUNT(d) DESC")
	List<Object[]> findDiseaseCounts(Pageable pageable);

	@Query("SELECT YEAR(d.diagnosisDate), MONTH(d.diagnosisDate), d.diseaseName, COUNT(d) FROM Diagnostics d GROUP BY YEAR(d.diagnosisDate), MONTH(d.diagnosisDate), d.diseaseName ORDER BY YEAR(d.diagnosisDate), MONTH(d.diagnosisDate)")
	List<Object[]> findDiagnosticsCountByMonthAndDisease();

	long countByDiagnosisDateAfter(LocalDateTime date);

	long countByDiagnosisDateBetween(LocalDateTime start, LocalDateTime end);
}
