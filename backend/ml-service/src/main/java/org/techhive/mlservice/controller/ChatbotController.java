package org.techhive.mlservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.mlservice.dto.ChatRequestDTO;
import org.techhive.mlservice.dto.ChatResponseDTO;
import org.techhive.mlservice.dto.StressAnalysisDTO;
import org.techhive.mlservice.entity.ChatMessage;
import org.techhive.mlservice.entity.ChatSession;
import org.techhive.mlservice.entity.CaregiverStressHistory;
import org.techhive.mlservice.service.StressDetectionService;
import org.techhive.mlservice.service.ChatbotService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final StressDetectionService stressDetectionService;

    // ENDPOINT DE TEST
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
    public StressAnalysisDTO getStressAnalysis(@PathVariable String userId) {
        return stressDetectionService.analyzeStress(userId);
    }

    // Récupérer l'historique du stress
    @GetMapping("/stress/history/{userId}")
    public List<CaregiverStressHistory> getStressHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "30") int days) {
        return stressDetectionService.getStressHistory(userId, days);
    }

    // Récupérer le dernier score de stress
    @GetMapping("/stress/latest/{userId}")
    public CaregiverStressHistory getLatestStress(@PathVariable String userId) {
        return stressDetectionService.getLatestStress(userId);
    }

    // Récupérer la tendance du stress
    @GetMapping("/stress/trend/{userId}")
    public Map<String, String> getStressTrend(@PathVariable String userId) {
        Map<String, String> response = new HashMap<>();
        response.put("trend", stressDetectionService.getStressTrend(userId));
        response.put("userId", userId);
        return response;
    }

    // NOUVEAU : Ajouter manuellement une entrée de stress (pour tests)
    @PostMapping("/stress/history")
    public ResponseEntity<CaregiverStressHistory> addStressHistory(@RequestBody CaregiverStressHistory stress) {
        if (stress.getCreatedAt() == null) {
            stress.setCreatedAt(LocalDateTime.now());
        }
        CaregiverStressHistory saved = stressDetectionService.saveStressHistory(stress);
        return ResponseEntity.ok(saved);
    }
}