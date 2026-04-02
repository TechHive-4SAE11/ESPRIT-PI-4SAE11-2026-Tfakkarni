package org.techhive.medicalservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.medicalservice.entity.AIReport;

public interface AIReportRepository extends JpaRepository<AIReport, Long> {

	List<AIReport> findByMedicalFolderIdOrderByGeneratedAtDesc(Long medicalFolderId);

	Optional<AIReport> findFirstByMedicalFolderIdOrderByGeneratedAtDesc(Long medicalFolderId);
}
