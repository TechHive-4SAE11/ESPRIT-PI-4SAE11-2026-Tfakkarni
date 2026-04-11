package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientQuestionDTO {
    private String id;
    private String questionText;
    private String type; // e.g., "YES_NO", "SCALE", "CHOICE"
    private List<String> options;
}
