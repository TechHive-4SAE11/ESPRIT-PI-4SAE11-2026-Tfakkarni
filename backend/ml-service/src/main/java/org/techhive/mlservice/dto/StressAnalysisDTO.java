package org.techhive.mlservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StressAnalysisDTO {
    private String stressLevel;
    private List<String> factors;
    private String recommendation;
}

