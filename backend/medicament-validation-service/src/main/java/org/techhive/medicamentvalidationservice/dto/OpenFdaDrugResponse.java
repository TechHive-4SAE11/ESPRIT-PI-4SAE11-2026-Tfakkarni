package org.techhive.medicamentvalidationservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for deserializing responses from the OpenFDA Drug API.
 * See: https://api.fda.gov/drug/drugsfda.json
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenFdaDrugResponse {

    private Meta meta;
    private List<DrugResult> results;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("results")
        private ResultsMeta resultsMeta;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ResultsMeta {
            private int skip;
            private int limit;
            private int total;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DrugResult {
        @JsonProperty("application_number")
        private String applicationNumber;

        private List<Product> products;
        private OpenFdaInfo openfda;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Product {
        @JsonProperty("brand_name")
        private String brandName;

        @JsonProperty("active_ingredients")
        private List<ActiveIngredient> activeIngredients;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActiveIngredient {
        private String name;
        private String strength;
    }

    @Data
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
}
