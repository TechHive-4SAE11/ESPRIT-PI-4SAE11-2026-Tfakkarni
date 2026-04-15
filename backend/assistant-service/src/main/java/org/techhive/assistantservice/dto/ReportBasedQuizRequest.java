package org.techhive.assistantservice.dto;

import lombok.Data;

@Data
public class ReportBasedQuizRequest {
    private String patientName;
    private Integer numberOfQuestions;
}
