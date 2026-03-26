package org.techhive.medicalservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.medicalservice.entity.MedicalHistory;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
	List<MedicalHistory> findByMedicalFolderId(Long medicalFolderId);
}
