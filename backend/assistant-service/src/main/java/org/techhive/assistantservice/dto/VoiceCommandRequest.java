package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCommandRequest {
    private String command;
    private Long userId;
    private String sessionId;
}
