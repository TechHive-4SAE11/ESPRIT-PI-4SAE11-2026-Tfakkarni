package org.techhive.medicalservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatientDTO {
    private Long id;
    private String keycloakId;
    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private String diagnosis;
}
