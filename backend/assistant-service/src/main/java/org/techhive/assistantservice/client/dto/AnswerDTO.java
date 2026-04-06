package org.techhive.assistantservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerDTO {
    private Long id;
    private String text;

    @JsonProperty("isCorrect")
    private Boolean isCorrect;

    private String explanation;
    private Long questionId;
}
