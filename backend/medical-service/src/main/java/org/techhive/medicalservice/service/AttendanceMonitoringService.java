package org.techhive.medicalservice.service;

/**
 * Evaluates appointment history (no-shows) and updates {@link org.techhive.medicalservice.entity.MedicalFolder} flags.
 */
public interface AttendanceMonitoringService {

    /**
     * Recomputes consecutive/total no-shows and restriction flags for the patient's folder(s).
     */
    void recalculateForPatient(String patientId);

    /**
     * Staff clears temporary booking restriction after manual review (does not delete folder).
     */
    void clearBookingRestrictionAfterReview(Long medicalFolderId);
}
