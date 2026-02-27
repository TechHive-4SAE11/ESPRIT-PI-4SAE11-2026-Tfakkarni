package org.techhive.medicamentvalidationservice.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.techhive.medicamentvalidationservice.dto.OpenFdaDrugResponse;
import org.techhive.medicamentvalidationservice.entity.ValidMedicament;

import java.util.stream.Collectors;

/**
 * Processes OpenFDA DrugResult into ValidMedicament entities.
 * Extracts brand name, generic name, and active ingredients.
 */
@Slf4j
@Component
public class MedicamentItemProcessor implements ItemProcessor<OpenFdaDrugResponse.DrugResult, ValidMedicament> {

    @Override
    public ValidMedicament process(OpenFdaDrugResponse.DrugResult item) {
        ValidMedicament medicament = new ValidMedicament();

        // Extract application number
        medicament.setApplicationNumber(item.getApplicationNumber());

        // Extract brand name (from openfda or products)
        String brandName = extractBrandName(item);
        medicament.setBrandName(brandName);

        // Extract generic name
        String genericName = extractGenericName(item);
        medicament.setGenericName(genericName);

        // Set drug name (prefer brand name, fall back to generic)
        String drugName = brandName != null ? brandName : genericName;
        if (drugName == null || drugName.isBlank()) {
            log.debug("Skipping drug record with no identifiable name: {}", item.getApplicationNumber());
            return null; // Skip records with no name (processor returning null = skip)
        }
        medicament.setDrugName(drugName.trim());

        // Extract active ingredients
        String activeIngredients = extractActiveIngredients(item);
        medicament.setActiveIngredients(activeIngredients);

        return medicament;
    }

    private String extractBrandName(OpenFdaDrugResponse.DrugResult item) {
        // Try openfda.brand_name first
        if (item.getOpenfda() != null && item.getOpenfda().getBrandName() != null && !item.getOpenfda().getBrandName().isEmpty()) {
            return item.getOpenfda().getBrandName().get(0);
        }
        // Fall back to products.brand_name
        if (item.getProducts() != null && !item.getProducts().isEmpty()) {
            for (OpenFdaDrugResponse.Product product : item.getProducts()) {
                if (product.getBrandName() != null && !product.getBrandName().isBlank()) {
                    return product.getBrandName();
                }
            }
        }
        return null;
    }

    private String extractGenericName(OpenFdaDrugResponse.DrugResult item) {
        if (item.getOpenfda() != null && item.getOpenfda().getGenericName() != null && !item.getOpenfda().getGenericName().isEmpty()) {
            return item.getOpenfda().getGenericName().get(0);
        }
        return null;
    }

    private String extractActiveIngredients(OpenFdaDrugResponse.DrugResult item) {
        // Try openfda.substance_name
        if (item.getOpenfda() != null && item.getOpenfda().getSubstanceName() != null && !item.getOpenfda().getSubstanceName().isEmpty()) {
            return String.join(", ", item.getOpenfda().getSubstanceName());
        }
        // Fall back to products.active_ingredients
        if (item.getProducts() != null) {
            return item.getProducts().stream()
                    .filter(p -> p.getActiveIngredients() != null)
                    .flatMap(p -> p.getActiveIngredients().stream())
                    .map(OpenFdaDrugResponse.ActiveIngredient::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
        }
        return null;
    }
}
