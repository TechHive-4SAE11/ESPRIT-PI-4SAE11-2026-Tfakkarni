package org.techhive.medicamentvalidationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.medicamentvalidationservice.dto.BatchValidationResultDTO;
import org.techhive.medicamentvalidationservice.dto.ValidationResultDTO;
import org.techhive.medicamentvalidationservice.repository.ValidMedicamentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for validating medicament names against the loaded FDA drug database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicamentValidationService {

    private final ValidMedicamentRepository repository;

    /**
     * Validate a single drug name against the database.
     */
    public ValidationResultDTO validateDrugName(String name) {
        if (name == null || name.isBlank()) {
            return new ValidationResultDTO(name, false, "Drug name cannot be empty");
        }

        String trimmedName = name.trim();

        // Check if the drug exists by any name field
        boolean exists = repository.existsByAnyNameIgnoreCase(trimmedName);

        if (exists) {
            log.debug("Drug '{}' found in database", trimmedName);
            return new ValidationResultDTO(trimmedName, true, "Valid medicament name");
        }

        // Not found — try to suggest similar names
        List<String> suggestions = findSuggestions(trimmedName);

        String message = "Medicament '" + trimmedName + "' not found in FDA approved drug database";
        if (!suggestions.isEmpty()) {
            message += ". Did you mean: " + String.join(", ", suggestions) + "?";
        }

        ValidationResultDTO result = new ValidationResultDTO(trimmedName, false, message);
        result.setSuggestions(suggestions);

        log.debug("Drug '{}' not found. Suggestions: {}", trimmedName, suggestions);
        return result;
    }

    /**
     * Validate multiple drug names at once.
     */
    public BatchValidationResultDTO validateBatch(List<String> names) {
        List<ValidationResultDTO> results = names.stream()
                .map(this::validateDrugName)
                .collect(Collectors.toList());

        int validCount = (int) results.stream().filter(ValidationResultDTO::isValid).count();
        int invalidCount = results.size() - validCount;

        return new BatchValidationResultDTO(results, results.size(), validCount, invalidCount);
    }

    /**
     * Find similar drug names for suggestions.
     */
    private List<String> findSuggestions(String name) {
        // Search with a minimum of 3 characters for meaningful results
        if (name.length() < 3) {
            return new ArrayList<>();
        }

        List<String> drugNames = repository.findSimilarDrugNames(name);
        List<String> brandNames = repository.findSimilarBrandNames(name);
        List<String> genericNames = repository.findSimilarGenericNames(name);

        // Combine and deduplicate, limit to 5 suggestions
        return Stream.of(drugNames, brandNames, genericNames)
                .flatMap(List::stream)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Get total count of loaded medicaments.
     */
    public long getMedicamentCount() {
        return repository.count();
    }
}
