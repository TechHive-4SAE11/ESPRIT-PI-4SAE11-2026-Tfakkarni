package org.techhive.medicalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.medicalservice.entity.Medication;
import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

	List<Medication> findByPrescriptionId(Long prescriptionId);
}
