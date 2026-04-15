package org.techhive.mlservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.mlservice.entity.ChatSession;
import org.techhive.mlservice.repository.ChatSessionRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @InjectMocks
    private ChatbotService chatbotService;

    @Test
    void testChat_NewSession_ShouldCreateSessionAndReturnResponse() {
        // Arrange
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String result = chatbotService.chat(1L, "Comment gérer le stress ?", null);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("stress"));
        verify(chatSessionRepository, atLeast(1)).save(any(ChatSession.class));
    }

    @Test
    void testChat_ExistingSession_ShouldUseExistingSession() {
        // Arrange
        ChatSession existingSession = new ChatSession();
        existingSession.setId(1L);

        when(chatSessionRepository.findById(1L)).thenReturn(Optional.of(existingSession));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String result = chatbotService.chat(1L, "Qu'est-ce que l'Alzheimer ?", 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("alzheimer") || result.toLowerCase().contains("mémoire"));
        verify(chatSessionRepository, times(1)).findById(1L);
    }

    @Test
    void testChat_StressQuestion_ShouldReturnStressResponse() {
        // Arrange
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String result = chatbotService.chat(1L, "Je suis très stressé", null);

        // Assert
        assertTrue(result.contains("stress"));
        assertTrue(result.contains("5 minutes"));
    }

    @Test
    void testChat_AlzheimerQuestion_ShouldReturnAlzheimerResponse() {
        // Arrange
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String result = chatbotService.chat(1L, "C'est quoi Alzheimer ?", null);

        // Assert
        assertTrue(result.toLowerCase().contains("alzheimer") || result.toLowerCase().contains("mémoire"));
    }

    @Test
    void testChat_FoodQuestion_ShouldReturnFoodResponse() {
        // Arrange
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String result = chatbotService.chat(1L, "Mon proche refuse de manger", null);

        // Assert
        assertTrue(result.contains("repas") || result.contains("manger"));
        assertTrue(result.contains("frequents"));
    }

    @Test
    void testChat_UnknownQuestion_ShouldReturnDefaultResponse() {
        // Arrange
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String result = chatbotService.chat(1L, "Quel temps fait-il ?", null);

        // Assert
        assertTrue(result.contains("aider") || result.contains("preciser"));
    }
}
