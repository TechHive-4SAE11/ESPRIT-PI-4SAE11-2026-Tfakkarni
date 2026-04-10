package org.techhive.mlservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDTO {
    private int completedModules;
    private int totalModules;
    private double percentage;
}

