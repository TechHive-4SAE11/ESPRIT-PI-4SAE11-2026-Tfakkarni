package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for receiving medicament validation results from the validation service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentValidationResultDTO {

    private String drugName;
    private boolean valid;
    private String message;
    private List<String> suggestions = new ArrayList<>();
}
