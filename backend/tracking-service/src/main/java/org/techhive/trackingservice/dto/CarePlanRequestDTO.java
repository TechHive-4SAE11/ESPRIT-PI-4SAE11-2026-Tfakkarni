package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanRequestDTO {

    @NotNull(message = "Please select a consultation session")
    private Long sessionId;

    @NotEmpty(message = "At least one care activity is required. Please add activities to the care plan.")
    @Valid
    private List<CareActivityRequestDTO> activities = new ArrayList<>();
}
