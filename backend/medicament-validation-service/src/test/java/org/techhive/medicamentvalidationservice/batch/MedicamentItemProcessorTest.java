package org.techhive.medicamentvalidationservice.batch;

import org.junit.jupiter.api.Test;
import org.techhive.medicamentvalidationservice.dto.OpenFdaDrugResponse;
import org.techhive.medicamentvalidationservice.entity.ValidMedicament;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MedicamentItemProcessorTest {

    private final MedicamentItemProcessor processor = new MedicamentItemProcessor();

    @Test
    void shouldPreferOpenFdaBrandNameAndJoinSubstances() {
        OpenFdaDrugResponse.DrugResult drug = new OpenFdaDrugResponse.DrugResult();
        drug.setApplicationNumber("NDA123456");

        OpenFdaDrugResponse.OpenFdaInfo openfda = new OpenFdaDrugResponse.OpenFdaInfo();
        openfda.setBrandName(List.of(" Doliprane "));
        openfda.setGenericName(List.of("Paracetamol"));
        openfda.setSubstanceName(List.of("Acetaminophen", "Caffeine"));
        drug.setOpenfda(openfda);

        ValidMedicament result = processor.process(drug);

        assertThat(result).isNotNull();
        assertThat(result.getApplicationNumber()).isEqualTo("NDA123456");
        assertThat(result.getBrandName()).isEqualTo(" Doliprane ");
        assertThat(result.getGenericName()).isEqualTo("Paracetamol");
        assertThat(result.getDrugName()).isEqualTo("Doliprane");
        assertThat(result.getActiveIngredients()).isEqualTo("Acetaminophen, Caffeine");
    }

    @Test
    void shouldUseProductBrandAndDistinctProductIngredientsWhenOpenFdaNamesMissing() {
        OpenFdaDrugResponse.DrugResult drug = new OpenFdaDrugResponse.DrugResult();
        drug.setApplicationNumber("ANDA987654");

        OpenFdaDrugResponse.Product namelessProduct = new OpenFdaDrugResponse.Product();
        namelessProduct.setBrandName(" ");

        OpenFdaDrugResponse.ActiveIngredient ibuprofen = new OpenFdaDrugResponse.ActiveIngredient();
        ibuprofen.setName("Ibuprofen");
        OpenFdaDrugResponse.ActiveIngredient duplicateIbuprofen = new OpenFdaDrugResponse.ActiveIngredient();
        duplicateIbuprofen.setName("Ibuprofen");
        OpenFdaDrugResponse.ActiveIngredient blank = new OpenFdaDrugResponse.ActiveIngredient();
        blank.setName(" ");

        OpenFdaDrugResponse.Product namedProduct = new OpenFdaDrugResponse.Product();
        namedProduct.setBrandName("Advil");
        namedProduct.setActiveIngredients(List.of(ibuprofen, duplicateIbuprofen, blank));

        drug.setProducts(List.of(namelessProduct, namedProduct));

        ValidMedicament result = processor.process(drug);

        assertThat(result).isNotNull();
        assertThat(result.getDrugName()).isEqualTo("Advil");
        assertThat(result.getBrandName()).isEqualTo("Advil");
        assertThat(result.getGenericName()).isNull();
        assertThat(result.getActiveIngredients()).isEqualTo("Ibuprofen");
    }

    @Test
    void shouldUseGenericNameWhenBrandNameMissing() {
        OpenFdaDrugResponse.DrugResult drug = new OpenFdaDrugResponse.DrugResult();
        OpenFdaDrugResponse.OpenFdaInfo openfda = new OpenFdaDrugResponse.OpenFdaInfo();
        openfda.setGenericName(List.of("Amoxicillin"));
        drug.setOpenfda(openfda);

        ValidMedicament result = processor.process(drug);

        assertThat(result).isNotNull();
        assertThat(result.getDrugName()).isEqualTo("Amoxicillin");
        assertThat(result.getBrandName()).isNull();
        assertThat(result.getGenericName()).isEqualTo("Amoxicillin");
        assertThat(result.getActiveIngredients()).isNull();
    }

    @Test
    void shouldSkipDrugRecordWhenNoUsableNameExists() {
        OpenFdaDrugResponse.DrugResult drug = new OpenFdaDrugResponse.DrugResult();
        drug.setApplicationNumber("NO_NAME");

        OpenFdaDrugResponse.Product product = new OpenFdaDrugResponse.Product();
        product.setBrandName(" ");
        drug.setProducts(List.of(product));

        assertThat(processor.process(drug)).isNull();
    }
}
