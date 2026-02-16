package org.techhive.trackingservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.trackingservice.entity.Medication;
import org.techhive.trackingservice.entity.Prescription;
import org.techhive.trackingservice.repository.PrescriptionRepository;
import org.techhive.trackingservice.repository.SessionRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final SessionRepository sessionRepository;

    public Prescription createPrescription(Prescription prescription) {
        // Set bidirectional relationship for medications
        if (prescription.getMedications() != null) {
            for (Medication medication : prescription.getMedications()) {
                medication.setPrescription(prescription);
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
                        }
                    }
                    return prescriptionRepository.save(prescription);
                })
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));
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

    public Prescription updatePrescription(Long id, Prescription prescription) {
        return prescriptionRepository.findById(id)
                .map(existing -> {
                    // Clear existing medications
                    existing.getMedications().clear();
                    
                    // Add new medications and set bidirectional relationship
                    if (prescription.getMedications() != null) {
                        for (Medication medication : prescription.getMedications()) {
                            medication.setPrescription(existing);
                            existing.getMedications().add(medication);
                        }
                    }
                    
                    return prescriptionRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + id));
    }

    public void deletePrescription(Long id) {
        prescriptionRepository.deleteById(id);
    }
}
