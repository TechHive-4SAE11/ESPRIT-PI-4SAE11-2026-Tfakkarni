package org.techhive.trackingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.techhive.trackingservice.entity.FollowUpReminder;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FollowUpReminderRepository extends JpaRepository<FollowUpReminder, Long> {

    /** All reminders for a patient, newest first */
    List<FollowUpReminder> findByPatientKeycloakIdOrderByCreatedAtDesc(String patientKeycloakId);

    /** Unread reminders only */
    List<FollowUpReminder> findByPatientKeycloakIdAndReadFalseOrderByCreatedAtDesc(String patientKeycloakId);

    /** Number of unread reminders */
    long countByPatientKeycloakIdAndReadFalse(String patientKeycloakId);

    /** Idempotency — avoid duplicate reminders on the same day */
    boolean existsByPatientKeycloakIdAndReminderDate(String patientKeycloakId, LocalDate reminderDate);

    /**
     * ✅ FIXED — Get ALL patients registered in the platform (via medical_folders).
     * Previously only returned patients with ACTIVE medications → missed many patients.
     * Now returns every distinct patient who has a medical folder.
     */
    @Query("SELECT DISTINCT f.idPatient FROM MedicalFolder f")
    List<String> findAllRegisteredPatientIds();

    /**
     * Legacy query — patients with active medications only.
     * Kept for reference but no longer used by the scheduler.
     */
    @Query("SELECT DISTINCT m.prescription.session.medicalFolder.idPatient " +
           "FROM Medication m " +
           "WHERE m.status IN " +
           "(org.techhive.trackingservice.enums.MedicationStatus.ACTIVE, " +
           " org.techhive.trackingservice.enums.MedicationStatus.ONGOING)")
    List<String> findAllActivePatientIds();
}
