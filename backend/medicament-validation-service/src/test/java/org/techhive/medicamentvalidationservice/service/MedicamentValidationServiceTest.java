package org.techhive.medicamentvalidationservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicamentvalidationservice.dto.BatchValidationResultDTO;
import org.techhive.medicamentvalidationservice.dto.ValidationResultDTO;
import org.techhive.medicamentvalidationservice.repository.ValidMedicamentRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicamentValidationServiceTest {

    @Mock
    private ValidMedicamentRepository repository;

    @InjectMocks
    private MedicamentValidationService validationService;

    @Nested
    @DisplayName("Tests de validation de médicament unique")
    class SingleValidationTests {

        @Test
        @DisplayName("Devrait valider un médicament existant (ignorer la casse)")
        void shouldValidateExistingMedicament() {
            // Given
            String name = "Doliprane";
            when(repository.existsByAnyNameIgnoreCase(name)).thenReturn(true);

            // When
            ValidationResultDTO result = validationService.validateDrugName(name);

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getDrugName()).isEqualTo(name);
            assertThat(result.getMessage()).contains("Valid");
            verify(repository, never()).findSimilarDrugNames(anyString());
        }

        @Test
        @DisplayName("Devrait échouer pour un nom vide ou nul")
        void shouldFailForEmptyName() {
            // When/Then
            assertThat(validationService.validateDrugName(null).isValid()).isFalse();
            assertThat(validationService.validateDrugName("").isValid()).isFalse();
            assertThat(validationService.validateDrugName("   ").isValid()).isFalse();
        }

        @Test
        @DisplayName("Devrait retourner invalide avec suggestions si non trouvé")
        void shouldReturnInvalidWithSuggestions() {
            // Given
            String name = "Asprine"; // Faute de frappe
            when(repository.existsByAnyNameIgnoreCase(name)).thenReturn(false);
            when(repository.findSimilarDrugNames(name)).thenReturn(List.of("Aspirin"));
            when(repository.findSimilarBrandNames(name)).thenReturn(List.of("Aspercreme"));
            when(repository.findSimilarGenericNames(name)).thenReturn(Collections.emptyList());

            // When
            ValidationResultDTO result = validationService.validateDrugName(name);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getSuggestions()).contains("Aspirin", "Aspercreme");
            assertThat(result.getMessage()).contains("not found");
            assertThat(result.getMessage()).contains("Did you mean");
        }

        @Test
        @DisplayName("Devrait dedupliquer et limiter les suggestions")
        void shouldDeduplicateAndLimitSuggestions() {
            String name = "Metformn";
            when(repository.existsByAnyNameIgnoreCase(name)).thenReturn(false);
            when(repository.findSimilarDrugNames(name)).thenReturn(List.of("Metformin", "Metformin XR", "Metformin"));
            when(repository.findSimilarBrandNames(name)).thenReturn(List.of("Glucophage", "Fortamet", "Metformin"));
            when(repository.findSimilarGenericNames(name)).thenReturn(List.of("Riomet", "Extra suggestion"));

            ValidationResultDTO result = validationService.validateDrugName(name);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getSuggestions())
                    .containsExactly("Metformin", "Metformin XR", "Glucophage", "Fortamet", "Riomet");
            assertThat(result.getMessage()).contains("Did you mean");
        }

        @Test
        @DisplayName("Ne pas chercher de suggestions si le nom est trop court (< 3)")
        void shouldNotProvideSuggestionsForTooShortName() {
            // Given
            String name = "Ab";
            when(repository.existsByAnyNameIgnoreCase(name)).thenReturn(false);

            // When
            ValidationResultDTO result = validationService.validateDrugName(name);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getSuggestions()).isEmpty();
            verify(repository, never()).findSimilarDrugNames(anyString());
        }
    }

    @Nested
    @DisplayName("Tests de validation par lot (Batch)")
    class BatchValidationTests {

        @Test
        @DisplayName("Devrait traiter correctement une liste mixte")
        void shouldProcessMixedBatch() {
            // Given
            List<String> names = List.of("Doliprane", "Inconnu");
            when(repository.existsByAnyNameIgnoreCase("Doliprane")).thenReturn(true);
            when(repository.existsByAnyNameIgnoreCase("Inconnu")).thenReturn(false);

            // When
            BatchValidationResultDTO result = validationService.validateBatch(names);

            // Then
            assertThat(result.getTotalCount()).isEqualTo(2);
            assertThat(result.getValidCount()).isEqualTo(1);
            assertThat(result.getInvalidCount()).isEqualTo(1);
            assertThat(result.getResults().get(0).isValid()).isTrue();
            assertThat(result.getResults().get(1).isValid()).isFalse();
        }
    }

    @Test
    @DisplayName("Devrait retourner le nombre total de médicaments")
    void shouldReturnMedicamentCount() {
        // Given
        when(repository.count()).thenReturn(5000L);

        // When/Then
        assertThat(validationService.getMedicamentCount()).isEqualTo(5000L);
    }
}
