package org.techhive.assistantservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.techhive.assistantservice.dto.VoiceCommandRequest;
import org.techhive.assistantservice.dto.VoiceCommandResponse;
import org.techhive.assistantservice.service.VoiceAssistantService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class VoiceAssistantWebSocketController {

    private final VoiceAssistantService voiceAssistantService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket endpoint for voice commands.
     * Client sends to: /app/voice-command
     * Response sent to: /topic/voice-response
     */
    @MessageMapping("/voice-command")
    @SendTo("/topic/voice-response")
    public VoiceCommandResponse handleVoiceCommand(@Payload VoiceCommandRequest request,
                                                    SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket voice command from session {}: '{}'", sessionId, request.getCommand());

        request.setSessionId(sessionId);

        VoiceCommandResponse response = voiceAssistantService.processCommand(request);
        response.setSessionId(sessionId);

        return response;
    }

    /**
     * Send a message to a specific user session.
     */
    public void sendToUser(String sessionId, VoiceCommandResponse response) {
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/voice-response", response);
    }
}
