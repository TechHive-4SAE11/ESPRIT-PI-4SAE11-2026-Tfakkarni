package org.techhive.assistantservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.dto.VoiceCommandRequest;
import org.techhive.assistantservice.dto.VoiceCommandResponse;
import org.techhive.assistantservice.service.VoiceAssistantService;

import java.util.Map;

/**
 * REST fallback controller for voice commands (when WebSocket is not available).
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/assistant")
@RequiredArgsConstructor
public class VoiceAssistantRestController {

    private final VoiceAssistantService voiceAssistantService;

    /**
     * POST /api/ai/assistant/command
     * Process a voice command via REST (fallback for WebSocket).
     */
    @PostMapping("/command")
    public ResponseEntity<VoiceCommandResponse> processCommand(@RequestBody VoiceCommandRequest request) {
        log.info("REST voice command: '{}' from user: {}", request.getCommand(), request.getUserId());

        VoiceCommandResponse response = voiceAssistantService.processCommand(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/ai/assistant/health
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "assistant-service",
                "status", "UP",
                "version", "1.0.0"
        ));
    }
}
