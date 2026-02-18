package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.Session;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    
    List<Session> findByMedicalFolderId(Long medicalFolderId);
    
    List<Session> findByMedicalFolderIdOrderBySessionDateDesc(Long medicalFolderId);

    List<Session> findByMedicalFolderIdAndPrescriptionsIsEmpty(Long medicalFolderId);

    List<Session> findByMedicalFolderIdAndCarePlansIsEmpty(Long medicalFolderId);
}
