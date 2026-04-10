package org.techhive.mlservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDTO {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String difficulty;
    private Integer duration;
}
