package org.techhive.medicalservice.service;

import org.techhive.medicalservice.dto.PatientBadgeDto;
import java.util.List;

public interface PatientBadgeService {

    /** Retourne tous les badges d'un patient, du plus récent au plus ancien. */
    List<PatientBadgeDto> getBadgesForPatient(String patientId);

    /**
     * Évalue les performances du patient (via game-service) et attribue
     * les badges mérités qui n'existent pas encore.
     * @return la liste des NOUVEAUX badges attribués lors de cet appel.
     */
    List<PatientBadgeDto> evaluateAndAwardBadges(String patientId);
}
