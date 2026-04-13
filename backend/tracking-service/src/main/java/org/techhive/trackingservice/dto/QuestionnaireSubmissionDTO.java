package org.techhive.trackingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionnaireSubmissionDTO {
    private Long patientId;
    private List<PatientAnswerDTO> answers;
}
