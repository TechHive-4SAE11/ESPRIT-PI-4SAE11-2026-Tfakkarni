package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.techhive.assistantservice.client.GameServiceClient;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.client.dto.EquipmentDTO;
import org.techhive.assistantservice.client.dto.EquipmentLoanDTO;
import org.techhive.assistantservice.dto.VoiceCommandRequest;
import org.techhive.assistantservice.dto.VoiceCommandResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceAssistantServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private GameServiceClient gameServiceClient;

    @Mock
    private MedicalServiceClient medicalServiceClient;

    @Mock
    private QuizAIService quizAIService;

    @Mock
    private VideoScriptService videoScriptService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VoiceAssistantService voiceAssistantService;

    private VoiceCommandRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = VoiceCommandRequest.builder()
                .command("borrow wheelchair")
                .userId(10L)
                .sessionId("session-123")
                .build();
    }

    @Test
    void processCommand_borrowCommand_shouldSearchAndBorrow() throws Exception {
        // Mock AI intent classification
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("{\"action\": \"BORROW\", \"parameter\": \"wheelchair\"}");

        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(java.util.Map.of("action", "BORROW", "parameter", "wheelchair"));

        // Mock equipment search
        EquipmentDTO equipment = EquipmentDTO.builder()
                .id(1L)
                .name("Wheelchair")
                .status("AVAILABLE")
                .build();
        when(medicalServiceClient.searchEquipment("wheelchair")).thenReturn(List.of(equipment));

        // Mock borrow
        EquipmentLoanDTO createdLoan = EquipmentLoanDTO.builder().id(1L).build();
        when(medicalServiceClient.borrowEquipment(any())).thenReturn(createdLoan);

        VoiceCommandResponse response = voiceAssistantService.processCommand(sampleRequest);

        assertNotNull(response);
        assertEquals("ACTION", response.getType());
        assertTrue(response.getMessage().contains("borrowed successfully"));
    }

    @Test
    void processCommand_statusCommand_shouldReturnStats() throws Exception {
        VoiceCommandRequest statusRequest = VoiceCommandRequest.builder()
                .command("status")
                .userId(10L)
                .sessionId("session-123")
                .build();

        // Mock AI classification
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("{\"action\": \"STATUS\", \"parameter\": \"\"}");

        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(java.util.Map.of("action", "STATUS", "parameter", ""));

        // Mock game-service stats
        when(gameServiceClient.getQuizCountByCaregiver(10L)).thenReturn(5L);
        when(gameServiceClient.getAverageScoreByCaregiver(10L)).thenReturn(75.0);
        when(gameServiceClient.getWeakTopicsByCaregiver(10L)).thenReturn(List.of("Geography"));

        // Mock medical-service stats
        when(medicalServiceClient.getActiveLoansByBorrower(10L)).thenReturn(List.of());

        VoiceCommandResponse response = voiceAssistantService.processCommand(statusRequest);

        assertNotNull(response);
        assertEquals("INFO", response.getType());
        assertTrue(response.getMessage().contains("Status"));
    }

    @Test
    void processCommand_whenAIClassificationFails_shouldReturnError() {
        VoiceCommandRequest badRequest = VoiceCommandRequest.builder()
                .command("invalid gibberish")
                .userId(10L)
                .sessionId("session-123")
                .build();

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("API timeout"));

        VoiceCommandResponse response = voiceAssistantService.processCommand(badRequest);

        assertNotNull(response);
        assertEquals("ERROR", response.getType());
    }

    @Test
    void processCommand_returnWithNoActiveLoans_shouldReturnInfo() throws Exception {
        VoiceCommandRequest returnRequest = VoiceCommandRequest.builder()
                .command("return wheelchair")
                .userId(10L)
                .sessionId("session-123")
                .build();

        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("{\"action\": \"RETURN\", \"parameter\": \"wheelchair\"}");

        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(java.util.Map.of("action", "RETURN", "parameter", "wheelchair"));

        when(medicalServiceClient.getActiveLoansByBorrower(10L)).thenReturn(List.of());

        VoiceCommandResponse response = voiceAssistantService.processCommand(returnRequest);

        assertNotNull(response);
        assertEquals("INFO", response.getType());
        assertTrue(response.getMessage().contains("no active loans"));
    }
}
