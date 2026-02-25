package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.enums.MedicationStatus;
import org.techhive.trackingservice.repository.MedicationIntakeLogRepository;
import org.techhive.trackingservice.repository.PrescriptionRepository;
import org.techhive.trackingservice.repository.SessionRepository;
import org.techhive.trackingservice.util.DurationParser;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final SessionRepository sessionRepository;
    private final MedicationIntakeLogRepository medicationIntakeLogRepository;

    public Prescription createPrescription(Prescription prescription) {
        // Set bidirectional relationship for medications
        if (prescription.getMedications() != null) {
            for (Medication medication : prescription.getMedications()) {
                medication.setPrescription(prescription);
                initializeMedicationDates(medication, prescription);
            }
        }
        return prescriptionRepository.save(prescription);
    }

    public Prescription createPrescriptionForSession(Long sessionId, Prescription prescription) {
        return sessionRepository.findById(sessionId)
                .map(session -> {
                    prescription.setSession(session);
                    // Set bidirectional relationship for medications
                    if (prescription.getMedications() != null) {
                        for (Medication medication : prescription.getMedications()) {
                            medication.setPrescription(prescription);
                            initializeMedicationDates(medication, prescription);
                        }
                    }
                    return prescriptionRepository.save(prescription);
                })
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
    }

    /**
     * Initialize medication dates and status based on session date and duration
     */
    private void initializeMedicationDates(Medication medication, Prescription prescription) {
        // Set start date from session date
        if (prescription.getSession() != null && prescription.getSession().getSessionDate() != null) {
            LocalDate startDate = prescription.getSession().getSessionDate().toLocalDate();
            medication.setStartDate(startDate);
            
            // Calculate end date from duration
            if (medication.getDuration() != null) {
                LocalDate endDate = DurationParser.calculateEndDate(startDate, medication.getDuration());
                medication.setEndDate(endDate);
            }
            
            // Set initial status
            MedicationStatus status = DurationParser.determineStatus(
                medication.getStartDate(),
                medication.getEndDate(),
                LocalDate.now()
            );
            medication.setStatus(status);
        } else {
            // Default to ACTIVE if no session date available
            medication.setStatus(MedicationStatus.ACTIVE);
        }
    }

    @Transactional(readOnly = true)
    public List<Prescription> getAllPrescriptions() {
        return prescriptionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Prescription> getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsBySession(Long sessionId) {
        return prescriptionRepository.findBySessionId(sessionId);
    }

    @Transactional(readOnly = true)
    public List<Prescription> getPrescriptionsByPatient(String idPatient) {
        return prescriptionRepository.findBySessionMedicalFolderIdPatient(idPatient);
    }
    
    @Transactional(readOnly = true)
    public Page<Prescription> getPrescriptionsByPatientPaginated(String idPatient, Pageable pageable) {
        return prescriptionRepository.findBySessionMedicalFolderIdPatient(idPatient, pageable);
    }

    @Transactional
    public Prescription updatePrescription(Long id, Prescription prescription) {
        return prescriptionRepository.findById(id)
                .map(existing -> {
                    // Delete intake logs for existing medications before removing them
                    if (existing.getMedications() != null) {
                        for (Medication med : existing.getMedications()) {
                            medicationIntakeLogRepository.deleteByMedicationId(med.getId());
                        }
                    }

                    // Clear existing medications
                    existing.getMedications().clear();
                    
                    // Add new medications and set bidirectional relationship
                    if (prescription.getMedications() != null) {
                        for (Medication medication : prescription.getMedications()) {
                            medication.setPrescription(existing);
                            initializeMedicationDates(medication, existing);
                            existing.getMedications().add(medication);
                        }
                    }
                    
                    return prescriptionRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + id));
    }

    @Transactional
    public void deletePrescription(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + id));

        // Delete all intake logs associated with the medications of this prescription
        if (prescription.getMedications() != null) {
            for (Medication medication : prescription.getMedications()) {
                medicationIntakeLogRepository.deleteByMedicationId(medication.getId());
            }
        }

        prescriptionRepository.delete(prescription);
    }
}
