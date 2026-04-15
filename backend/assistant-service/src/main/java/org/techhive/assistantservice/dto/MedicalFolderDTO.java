package org.techhive.assistantservice.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedicalFolderDTO {
    private Long id;
    private String patientId;
    private String diagnosis;
    private String treatments;
    private String evolution;
    private List<String> weakPoints;
    private String recommendations;
}
