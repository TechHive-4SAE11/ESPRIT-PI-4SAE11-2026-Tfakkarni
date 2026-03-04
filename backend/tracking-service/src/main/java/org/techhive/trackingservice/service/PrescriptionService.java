package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.client.MedicamentValidationClient;
import org.techhive.trackingservice.dto.MedicamentValidationResultDTO;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.enums.MedicationStatus;
import org.techhive.trackingservice.repository.MedicationIntakeLogRepository;
import org.techhive.trackingservice.repository.PrescriptionRepository;
import org.techhive.trackingservice.repository.SessionRepository;
import org.techhive.trackingservice.util.DurationParser;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final SessionRepository sessionRepository;
    private final MedicationIntakeLogRepository medicationIntakeLogRepository;
    private final MedicamentValidationClient medicamentValidationClient;

    /**
     * Validate all medication names against the medicament-validation-service.
     * Validates against Tunisian (TN) drug database by default.
     * Throws IllegalArgumentException if any medication is invalid.
     */
    private void validateMedications(List<Medication> medications) {
        if (medications == null || medications.isEmpty()) {
            return;
        }

        List<String> invalidMedications = new ArrayList<>();

        for (Medication medication : medications) {
            if (medication.getMedicationName() != null && !medication.getMedicationName().isBlank()) {
                try {
                    MedicamentValidationResultDTO result = medicamentValidationClient
                            .validateMedicament(medication.getMedicationName());
                    if (!result.isValid()) {
                        String errorMsg = medication.getMedicationName();
                        if (result.getSuggestions() != null && !result.getSuggestions().isEmpty()) {
                            errorMsg += " (Did you mean: " + String.join(", ", result.getSuggestions()) + "?)";
                        }
                        invalidMedications.add(errorMsg);
                    }
                } catch (Exception e) {
                    log.warn("Could not validate medication '{}': {}. Allowing it through.",
                            medication.getMedicationName(), e.getMessage());
                }
            }
        }

        if (!invalidMedications.isEmpty()) {
            throw new IllegalArgumentException(
                    "The following medications are not recognized in the approved drug database: " +
                    String.join("; ", invalidMedications));
        }
    }

    public Prescription createPrescription(Prescription prescription) {
        // Validate medication names before saving
        validateMedications(prescription.getMedications());

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
        // Validate medication names before saving
        validateMedications(prescription.getMedications());

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

    /**
     * Resolve the doctor's keycloakId for a given prescription.
     * Must run inside a transaction to safely traverse LAZY relationships.
     */
    @Transactional(readOnly = true)
    public String getDoctorKeycloakIdForPrescription(Long prescriptionId) {
        return prescriptionRepository.findById(prescriptionId)
                .map(p -> p.getSession())
                .map(s -> s.getMedicalFolder())
                .map(mf -> mf.getIdDoctor())
                .orElse(null);
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
        // Validate medication names before updating
        validateMedications(prescription.getMedications());

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
