package org.techhive.mlservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.mlservice.entity.ChatMessage;
import org.techhive.mlservice.entity.ChatSession;
import org.techhive.mlservice.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatbotService {

    private final ChatSessionRepository chatSessionRepository;

    public String chat(Long userId, String question, Long sessionId) {
        ChatSession session;
        if (sessionId != null) {
            session = chatSessionRepository.findById(sessionId)
                    .orElseGet(() -> createNewSession(userId));
        } else {
            session = createNewSession(userId);
        }

        // Ajout du message utilisateur (sans id, auto-généré)
        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole("USER");
        userMessage.setContent(question);
        userMessage.setTimestamp(LocalDateTime.now());
        session.getMessages().add(userMessage);

        String answer = getMockResponse(question);

        // Ajout du message assistant
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setRole("ASSISTANT");
        assistantMessage.setContent(answer);
        assistantMessage.setTimestamp(LocalDateTime.now());
        session.getMessages().add(assistantMessage);

        chatSessionRepository.save(session);
        return answer;
    }

    private String getMockResponse(String question) {
        String q = question.toLowerCase();
        if (q.contains("stress")) {
            return "Pour gerer le stress, prenez 5 minutes de pause.";
        }
        if (q.contains("alzheimer")) {
            return "La maladie d'Alzheimer affecte la memoire. Soyez patient.";
        }
        if (q.contains("manger") || q.contains("repas")) {
            return "Proposez des petits repas frequents et restez calme.";
        }
        return "Je suis la pour vous aider. Pouvez-vous preciser votre question ?";
    }

    public List<ChatSession> getChatHistory(Long userId) {
        return chatSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private ChatSession createNewSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return chatSessionRepository.save(session);
    }
}