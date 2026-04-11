package org.techhive.analyticsservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.analyticsservice.entity.CognitiveDomainAnalysis;

import java.util.List;

public interface CognitiveDomainAnalysisRepository extends JpaRepository<CognitiveDomainAnalysis, Long> {
    List<CognitiveDomainAnalysis> findByPatientKeycloakId(String patientKeycloakId);
    void deleteByPatientKeycloakId(String patientKeycloakId);
}
