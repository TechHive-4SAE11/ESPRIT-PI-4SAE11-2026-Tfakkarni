package org.techhive.medicamentvalidationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchValidationResultDTO {

    private List<ValidationResultDTO> results;
    private int totalCount;
    private int validCount;
    private int invalidCount;
}
