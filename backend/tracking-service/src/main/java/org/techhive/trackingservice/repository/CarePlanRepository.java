package org.techhive.trackingservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.CarePlan;

import java.util.List;

@Repository
public interface CarePlanRepository extends JpaRepository<CarePlan, Long> {
    List<CarePlan> findBySessionId(Long sessionId);

    List<CarePlan> findBySessionMedicalFolderIdPatient(String idPatient);
    
    Page<CarePlan> findBySessionMedicalFolderIdPatient(String idPatient, Pageable pageable);
}
