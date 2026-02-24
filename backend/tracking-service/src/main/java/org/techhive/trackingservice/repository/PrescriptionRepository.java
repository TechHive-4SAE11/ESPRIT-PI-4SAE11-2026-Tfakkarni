package org.techhive.trackingservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.Prescription;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    
    List<Prescription> findBySessionId(Long sessionId);
    
    List<Prescription> findBySessionMedicalFolderIdPatient(String idPatient);
    
    Page<Prescription> findBySessionMedicalFolderIdPatient(String idPatient, Pageable pageable);
}
