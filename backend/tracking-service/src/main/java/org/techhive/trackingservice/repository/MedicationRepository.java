package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.Medication;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findByPrescriptionId(Long prescriptionId);

    List<Medication> findByPrescriptionSessionMedicalFolderIdPatient(String idPatient);

    List<Medication> findByPrescriptionSessionMedicalFolderIdDoctor(String idDoctor);
}
