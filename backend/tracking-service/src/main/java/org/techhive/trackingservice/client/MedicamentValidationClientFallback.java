package org.techhive.trackingservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.techhive.trackingservice.dto.MedicamentValidationResultDTO;

/**
 * Fallback for MedicamentValidationClient when the validation service is unavailable.
 * Returns valid=true to avoid blocking prescriptions when validation service is down.
 */
@Slf4j
@Component
public class MedicamentValidationClientFallback implements MedicamentValidationClient {

    @Override
    public MedicamentValidationResultDTO validateMedicament(String name) {
        log.warn("Medicament validation service is unavailable. Allowing medicament '{}' without validation.", name);
        MedicamentValidationResultDTO result = new MedicamentValidationResultDTO();
        result.setDrugName(name);
        result.setValid(true);
        result.setMessage("Validation service unavailable - medicament accepted without validation");
        return result;
    }
}
