package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NutritionEntryRequest {
    private String mealType;
    private String description;
    private String quantity;
    private String appetite;
    private Integer hydrationMl;
    private String notes;
    private String entryTime;
}
