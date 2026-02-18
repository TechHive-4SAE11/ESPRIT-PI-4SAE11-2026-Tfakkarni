package org.techhive.medicalservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.medicalservice.entity.Diagnostics;

@Repository
public interface DiagnosticsRepository extends JpaRepository<Diagnostics, Long> {
	List<Diagnostics> findByMedicalFolderId(Long medicalFolderId);
}
