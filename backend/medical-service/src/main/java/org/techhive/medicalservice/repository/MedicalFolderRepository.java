package org.techhive.medicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.medicalservice.entity.MedicalFolder;

@Repository
public interface MedicalFolderRepository extends JpaRepository<MedicalFolder, Long> {
}
