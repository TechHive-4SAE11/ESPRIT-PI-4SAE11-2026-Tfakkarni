package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCommandResponse {
    private String type;      // ACTION, INFO, ERROR, QUIZ_START
    private String message;
    private Object data;
    private String sessionId;
}
