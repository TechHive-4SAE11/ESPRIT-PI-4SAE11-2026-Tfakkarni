package org.techhive.trackingservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.enums.MedicationStatus;

import java.util.List;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findByPrescriptionId(Long prescriptionId);

    List<Medication> findByPrescriptionSessionMedicalFolderIdPatient(String idPatient);

    List<Medication> findByPrescriptionSessionMedicalFolderIdDoctor(String idDoctor);

    // Paginated queries for medications by patient
    Page<Medication> findByPrescriptionSessionMedicalFolderIdPatient(String idPatient, Pageable pageable);

    Page<Medication> findByPrescriptionSessionMedicalFolderIdPatientAndStatus(
            String idPatient,
            MedicationStatus status,
            Pageable pageable);

    // Paginated queries for medications by doctor
    Page<Medication> findByPrescriptionSessionMedicalFolderIdDoctor(String idDoctor, Pageable pageable);

    Page<Medication> findByPrescriptionSessionMedicalFolderIdDoctorAndStatus(
            String idDoctor,
            MedicationStatus status,
            Pageable pageable);
}
