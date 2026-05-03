package org.techhive.medicamentvalidationservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.techhive.medicamentvalidationservice.batch.MedicamentBatchScheduler;
import org.techhive.medicamentvalidationservice.dto.BatchValidationResultDTO;
import org.techhive.medicamentvalidationservice.dto.ValidationResultDTO;
import org.techhive.medicamentvalidationservice.service.MedicamentValidationService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MedicamentValidationControllerTest {

    private final MedicamentValidationService validationService = mock(MedicamentValidationService.class);
    private final MedicamentBatchScheduler batchScheduler = mock(MedicamentBatchScheduler.class);
    private final MedicamentValidationController controller = new MedicamentValidationController(validationService, batchScheduler);

    @Test
    void validateDrugShouldReturnServiceResult() {
        ValidationResultDTO expected = new ValidationResultDTO("Doliprane", true, "Valid medicament name");
        when(validationService.validateDrugName("Doliprane")).thenReturn(expected);

        ResponseEntity<ValidationResultDTO> response = controller.validateDrug("Doliprane");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void validateBatchShouldReturnBatchResult() {
        List<String> names = List.of("Doliprane", "Unknown");
        BatchValidationResultDTO expected = new BatchValidationResultDTO(List.of(), 2, 1, 1);
        when(validationService.validateBatch(names)).thenReturn(expected);

        ResponseEntity<BatchValidationResultDTO> response = controller.validateBatch(names);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isSameAs(expected);
    }

    @Test
    void getStatusShouldReturnReadyWhenDatabaseHasMedicaments() {
        when(validationService.getMedicamentCount()).thenReturn(42L);

        ResponseEntity<Map<String, Object>> response = controller.getStatus();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "READY");
        assertThat(response.getBody()).containsEntry("totalMedicaments", 42L);
        assertThat((String) response.getBody().get("message")).contains("42");
    }

    @Test
    void getStatusShouldReturnLoadingWhenDatabaseIsEmpty() {
        when(validationService.getMedicamentCount()).thenReturn(0L);

        ResponseEntity<Map<String, Object>> response = controller.getStatus();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "LOADING");
        assertThat(response.getBody()).containsEntry("totalMedicaments", 0L);
        assertThat((String) response.getBody().get("message")).contains("Database is empty");
    }

    @Test
    void refreshDatabaseShouldTriggerBatchScheduler() {
        ResponseEntity<Map<String, String>> response = controller.refreshDatabase();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("message", "Medicament database refresh triggered. Check /status for progress.");
        verify(batchScheduler).runJob();
    }
}
