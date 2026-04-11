package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.techhive.trackingservice.entity.PatientAnswer;
import java.util.List;

public interface PatientAnswerRepository extends JpaRepository<PatientAnswer, Long> {
    List<PatientAnswer> findByPatientId(Long patientId);
}
