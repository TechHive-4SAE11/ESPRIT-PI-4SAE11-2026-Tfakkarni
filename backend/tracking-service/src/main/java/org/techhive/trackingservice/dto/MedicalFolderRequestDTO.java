package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalFolderRequestDTO {

    private String patientId;
    private String doctorId;
    private String bloodType;
    private Double height;
    private Double weight;
}
