package org.techhive.mlservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.techhive.mlservice.dto.ChatRequestDTO;
import org.techhive.mlservice.dto.ChatResponseDTO;
import org.techhive.mlservice.dto.StressAnalysisDTO;
import org.techhive.mlservice.entity.ChatMessage;
import org.techhive.mlservice.entity.ChatSession;
import org.techhive.mlservice.service.StressDetectionService;
import org.techhive.mlservice.service.ChatbotService;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final StressDetectionService stressDetectionService;

    // ENDPOINT DE TEST - À SUPPRIMER PLUS TARD
    @GetMapping("/test")
    public String test() {
        return "✅ Le backend ml-service fonctionne correctement sur le port 18085 !";
    }

    @PostMapping("/chat")
    public ChatResponseDTO chat(@RequestBody ChatRequestDTO request, @RequestParam(required = false) Long userId) {
        Long resolvedUserId = userId != null ? userId : 1L;
        String answer = chatbotService.chat(resolvedUserId, request.getQuestion(), request.getSessionId());
        return new ChatResponseDTO(answer, request.getSessionId());
    }

    @GetMapping("/chat/history/{userId}")
    public List<ChatMessage> getChatHistory(@PathVariable Long userId) {
        List<ChatSession> sessions = chatbotService.getChatHistory(userId);
        return sessions.stream()
                .flatMap(session -> session.getMessages().stream())
                .collect(Collectors.toList());
    }

    @GetMapping("/stress/{userId}")
    public StressAnalysisDTO getStressAnalysis(@PathVariable Long userId) {
        return stressDetectionService.analyzeStress(String.valueOf(userId));
    }
}