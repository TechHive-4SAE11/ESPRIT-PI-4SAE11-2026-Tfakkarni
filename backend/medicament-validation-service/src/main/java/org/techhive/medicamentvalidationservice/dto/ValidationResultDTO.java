package org.techhive.medicamentvalidationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultDTO {

    private String drugName;
    private boolean valid;
    private String message;
    private List<String> suggestions = new ArrayList<>();

    public ValidationResultDTO(String drugName, boolean valid, String message) {
        this.drugName = drugName;
        this.valid = valid;
        this.message = message;
    }
}
