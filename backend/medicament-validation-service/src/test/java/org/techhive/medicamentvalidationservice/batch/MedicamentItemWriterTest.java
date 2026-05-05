package org.techhive.medicamentvalidationservice.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.techhive.medicamentvalidationservice.entity.ValidMedicament;
import org.techhive.medicamentvalidationservice.repository.ValidMedicamentRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicamentItemWriterTest {

    private final ValidMedicamentRepository repository = mock(ValidMedicamentRepository.class);
    private final MedicamentItemWriter writer = new MedicamentItemWriter(repository);

    @Test
    void shouldSaveOnlyMedicamentsThatDoNotAlreadyExist() throws Exception {
        ValidMedicament newMedicament = medicament("Doliprane");
        ValidMedicament duplicate = medicament("Advil");

        when(repository.existsByDrugNameIgnoreCase("Doliprane")).thenReturn(false);
        when(repository.existsByDrugNameIgnoreCase("Advil")).thenReturn(true);

        writer.write(new Chunk<>(newMedicament, duplicate));

        verify(repository).save(newMedicament);
        verify(repository, never()).save(duplicate);
    }

    @Test
    void shouldContinueWhenRepositorySaveFails() throws Exception {
        ValidMedicament failing = medicament("FailingMed");
        ValidMedicament saved = medicament("SavedMed");

        when(repository.existsByDrugNameIgnoreCase("FailingMed")).thenReturn(false);
        when(repository.save(failing)).thenThrow(new RuntimeException("database unavailable"));
        when(repository.existsByDrugNameIgnoreCase("SavedMed")).thenReturn(false);

        writer.write(new Chunk<>(failing, saved));

        verify(repository).save(failing);
        verify(repository).save(saved);
    }

    private ValidMedicament medicament(String drugName) {
        ValidMedicament medicament = new ValidMedicament();
        medicament.setDrugName(drugName);
        return medicament;
    }
}
