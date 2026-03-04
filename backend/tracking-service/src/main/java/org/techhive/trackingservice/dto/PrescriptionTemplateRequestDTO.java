package org.techhive.trackingservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionTemplateRequestDTO {

    @NotBlank(message = "Template name is required")
    private String name;

    private String description;

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    @NotEmpty(message = "At least one medication is required")
    @Valid
    private List<MedicationRequestDTO> medications = new ArrayList<>();
}
