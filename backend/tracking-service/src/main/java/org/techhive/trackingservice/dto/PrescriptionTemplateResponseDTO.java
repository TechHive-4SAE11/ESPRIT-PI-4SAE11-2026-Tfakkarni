package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionTemplateResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String doctorId;
    private List<TemplateMedicationDTO> medications = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
