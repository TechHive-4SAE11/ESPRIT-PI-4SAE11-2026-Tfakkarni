package org.techhive.medicamentvalidationservice.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.techhive.medicamentvalidationservice.entity.ValidMedicament;
import org.techhive.medicamentvalidationservice.repository.ValidMedicamentRepository;

/**
 * Writes ValidMedicament entities to the database.
 * Handles duplicates gracefully by checking before insert.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicamentItemWriter implements ItemWriter<ValidMedicament> {

    private final ValidMedicamentRepository repository;

    @Override
    public void write(Chunk<? extends ValidMedicament> chunk) {
        int saved = 0;
        int skipped = 0;

        for (ValidMedicament medicament : chunk) {
            try {
                // Skip duplicates
                if (!repository.existsByDrugNameIgnoreCase(medicament.getDrugName())) {
                    repository.save(medicament);
                    saved++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                log.warn("Failed to save medicament '{}': {}", medicament.getDrugName(), e.getMessage());
                skipped++;
            }
        }

        log.debug("Batch write complete: {} saved, {} skipped (duplicates/errors)", saved, skipped);
    }
}
