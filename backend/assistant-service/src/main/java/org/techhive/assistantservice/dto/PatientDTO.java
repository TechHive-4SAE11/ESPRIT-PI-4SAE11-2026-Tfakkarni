package org.techhive.assistantservice.dto;

import lombok.Data;

@Data
public class PatientDTO {
    private Long id;
    private String keycloakId;
    private String firstName;
    private String lastName;
    private String email;
    private Integer age;
    private String diagnosis;
    private Integer cognitiveLevel; // 1 (Facile), 2 (Moyen), 3 (Difficile)
}
