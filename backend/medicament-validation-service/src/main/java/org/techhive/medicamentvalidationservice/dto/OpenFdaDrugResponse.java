package org.techhive.medicamentvalidationservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO to deserialize OpenFDA drug API responses.
 * API: https://api.fda.gov/drug/drugsfda.json
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFdaDrugResponse {

    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("results")
    private List<DrugResult> results;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("results")
        private ResultsMeta resultsMeta;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultsMeta {
        private int skip;
        private int limit;
        private int total;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DrugResult {
        @JsonProperty("application_number")
        private String applicationNumber;

        @JsonProperty("openfda")
        private OpenFdaInfo openfda;

        @JsonProperty("products")
        private List<Product> products;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenFdaInfo {
        @JsonProperty("brand_name")
        private List<String> brandName;

        @JsonProperty("generic_name")
        private List<String> genericName;

        @JsonProperty("substance_name")
        private List<String> substanceName;

        @JsonProperty("manufacturer_name")
        private List<String> manufacturerName;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        @JsonProperty("brand_name")
        private String brandName;

        @JsonProperty("active_ingredients")
        private List<ActiveIngredient> activeIngredients;

        @JsonProperty("dosage_form")
        private String dosageForm;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActiveIngredient {
        private String name;
        private String strength;
    }
}
