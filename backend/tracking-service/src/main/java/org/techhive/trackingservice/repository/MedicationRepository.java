package org.techhive.trackingservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.enums.MedicationStatus;

import java.util.Collection;
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

    /**
     * One round-trip for dossier safety audit: all ACTIVE medication rows for the given patients.
     * Columns: [0] = patient id, [1] = lower(trim(medication_name)).
     */
    @Query(value = """
            SELECT mf.id_patient, LOWER(TRIM(m.medication_name))
            FROM medications m
            INNER JOIN prescriptions pr ON m.prescription_id = pr.id
            INNER JOIN sessions sess ON pr.session_id = sess.id
            INNER JOIN medical_folders mf ON sess.medical_folder_id = mf.id
            WHERE m.status = 'ACTIVE'
            AND NULLIF(TRIM(COALESCE(m.medication_name, '')), '') IS NOT NULL
            AND mf.id_patient IN (:patientIds)
            """, nativeQuery = true)
    List<Object[]> findActiveMedicationRowsForPatients(@Param("patientIds") Collection<String> patientIds);
}
