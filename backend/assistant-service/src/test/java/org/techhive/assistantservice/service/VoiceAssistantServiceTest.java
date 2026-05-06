package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.client.GameServiceClient;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.client.dto.EquipmentDTO;
import org.techhive.assistantservice.client.dto.EquipmentLoanDTO;
import org.techhive.assistantservice.controller.EquipmentAIController;
import org.techhive.assistantservice.controller.QuizAIController;
import org.techhive.assistantservice.dto.EquipmentRecommendResponse;
import org.techhive.assistantservice.dto.QuizGenerateRequest;
import org.techhive.assistantservice.dto.VideoGenerateResponse;
import org.techhive.assistantservice.dto.VoiceCommandRequest;
import org.techhive.assistantservice.dto.VoiceCommandResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    @Mock
    private QuizAIController quizAIController;

    @Mock
    private EquipmentAIController equipmentAIController;

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

    @Test
    void processCommand_borrowCommandWhenSearchFails_shouldReturnServiceError() throws Exception {
        classifyAs("BORROW", "walker");
        when(medicalServiceClient.searchEquipment("walker")).thenThrow(new RuntimeException("offline"));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("borrow walker"));

        assertEquals("ERROR", response.getType());
        assertTrue(response.getMessage().contains("Unable to contact"));
    }

    @Test
    void processCommand_borrowCommandWhenNoEquipmentFound_shouldReturnInfo() throws Exception {
        classifyAs("BORROW", "walker");
        when(medicalServiceClient.searchEquipment("walker")).thenReturn(List.of());

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("borrow walker"));

        assertEquals("INFO", response.getType());
        assertTrue(response.getMessage().contains("No equipment found"));
    }

    @Test
    void processCommand_borrowCommandWhenEquipmentUnavailable_shouldReturnInfo() throws Exception {
        classifyAs("BORROW", "walker");
        when(medicalServiceClient.searchEquipment("walker")).thenReturn(List.of(
                EquipmentDTO.builder().id(1L).name("Walker").status("LOANED").build()
        ));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("borrow walker"));

        assertEquals("INFO", response.getType());
        assertTrue(response.getMessage().contains("not currently available"));
    }

    @Test
    void processCommand_borrowCommandWhenLoanCreationFails_shouldReturnError() throws Exception {
        classifyAs("BORROW", "walker");
        when(medicalServiceClient.searchEquipment("walker")).thenReturn(List.of(
                EquipmentDTO.builder().id(1L).name("Walker").status("AVAILABLE").build()
        ));
        when(medicalServiceClient.borrowEquipment(any())).thenThrow(new RuntimeException("quota reached"));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("borrow walker"));

        assertEquals("ERROR", response.getType());
        assertTrue(response.getMessage().contains("Borrow failed"));
    }

    @Test
    void processCommand_returnCommandWhenLookupFails_shouldReturnError() throws Exception {
        classifyAs("RETURN", "walker");
        when(medicalServiceClient.getActiveLoansByBorrower(10L)).thenThrow(new RuntimeException("offline"));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("return walker"));

        assertEquals("ERROR", response.getType());
        assertTrue(response.getMessage().contains("Unable to retrieve"));
    }

    @Test
    void processCommand_returnCommandWhenLoanDoesNotMatch_shouldListActiveLoans() throws Exception {
        classifyAs("RETURN", "walker");
        when(medicalServiceClient.getActiveLoansByBorrower(10L)).thenReturn(List.of(
                EquipmentLoanDTO.builder().id(2L).equipmentId(99L).equipmentName(null).build()
        ));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("return walker"));

        assertEquals("INFO", response.getType());
        assertTrue(response.getMessage().contains("No loan matching"));
        assertTrue(response.getMessage().contains("ID:99"));
    }

    @Test
    void processCommand_returnCommandWhenMatchingLoanFound_shouldReturnEquipment() throws Exception {
        classifyAs("RETURN", "chair");
        EquipmentLoanDTO loan = EquipmentLoanDTO.builder().id(3L).equipmentName("Wheelchair").build();
        when(medicalServiceClient.getActiveLoansByBorrower(10L)).thenReturn(List.of(loan));
        when(medicalServiceClient.returnEquipment(3L)).thenReturn(loan);

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("return chair"));

        assertEquals("ACTION", response.getType());
        assertSame(loan, response.getData());
    }

    @Test
    void processCommand_returnCommandWhenReturnFails_shouldReturnError() throws Exception {
        classifyAs("RETURN", "chair");
        EquipmentLoanDTO loan = EquipmentLoanDTO.builder().id(3L).equipmentName("Wheelchair").build();
        when(medicalServiceClient.getActiveLoansByBorrower(10L)).thenReturn(List.of(loan));
        when(medicalServiceClient.returnEquipment(3L)).thenThrow(new RuntimeException("already returned"));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("return chair"));

        assertEquals("ERROR", response.getType());
        assertTrue(response.getMessage().contains("Return failed"));
    }

    @Test
    void processCommand_quizCommandWithBlankTopic_shouldUseGeneralMemoryDefaults() throws Exception {
        classifyAs("QUIZ", "null");
        QuizDTO quiz = QuizDTO.builder().id(7L).questions(null).build();
        when(quizAIService.generateQuiz(any(QuizGenerateRequest.class))).thenReturn(quiz);

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("quiz"));

        ArgumentCaptor<QuizGenerateRequest> captor = ArgumentCaptor.forClass(QuizGenerateRequest.class);
        verify(quizAIService).generateQuiz(captor.capture());
        assertEquals("General Memory", captor.getValue().getTopic());
        assertEquals(5, captor.getValue().getNumberOfQuestions());
        assertEquals("QUIZ_START", response.getType());
        assertTrue(response.getMessage().contains("0 questions"));
    }

    @Test
    void processCommand_quizCommandWhenGenerationFails_shouldReturnError() throws Exception {
        classifyAs("QUIZ", "family");
        when(quizAIService.generateQuiz(any(QuizGenerateRequest.class))).thenThrow(new RuntimeException("AI quota"));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("quiz family"));

        assertEquals("ERROR", response.getType());
        assertTrue(response.getMessage().contains("Unable to generate"));
    }

    @Test
    void processCommand_quizForPatientWithoutName_shouldAskForPatient() throws Exception {
        classifyAs("QUIZ_FOR_PATIENT", " ");

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("quiz for patient"));

        assertEquals("INFO", response.getType());
        assertTrue(response.getMessage().contains("specify the patient's name"));
    }

    @Test
    void processCommand_quizForPatientWhenControllerSucceeds_shouldReturnQuiz() throws Exception {
        classifyAs("QUIZ_FOR_PATIENT", "Amina");
        QuizDTO quiz = QuizDTO.builder().id(8L).questions(List.of()).build();
        ResponseEntity<?> responseEntity = ResponseEntity.ok(quiz);
        doReturn(responseEntity).when(quizAIController).generateQuizFromPatientName(any());

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("quiz for Amina"));

        assertEquals("QUIZ_START", response.getType());
        assertSame(quiz, response.getData());
    }

    @Test
    void processCommand_recommendedQuizWithoutPatientContext_shouldAskForSelection() throws Exception {
        classifyAs("RECOMMENDED_QUIZ", "");
        VoiceCommandRequest request = command("recommended quiz");
        request.setPatientName(" ");

        VoiceCommandResponse response = voiceAssistantService.processCommand(request);

        assertEquals("INFO", response.getType());
        assertTrue(response.getMessage().contains("select a patient"));
    }

    @Test
    void processCommand_recommendEquipmentWithPatientContext_shouldUsePatientRecommendation() throws Exception {
        classifyAs("RECOMMEND_EQUIPMENT", "mobility");
        VoiceCommandRequest request = command("recommend equipment");
        request.setPatientName("Amina");
        EquipmentRecommendResponse body = EquipmentRecommendResponse.builder().condition("mobility").build();
        ResponseEntity<?> responseEntity = ResponseEntity.ok(body);
        doReturn(responseEntity).when(equipmentAIController).recommendEquipmentFromPatientName(any());

        VoiceCommandResponse response = voiceAssistantService.processCommand(request);

        assertEquals("INFO", response.getType());
        assertSame(body, response.getData());
        verify(equipmentAIController).recommendEquipmentFromPatientName(Map.of("patientName", "Amina"));
    }

    @Test
    void processCommand_recommendEquipmentWithoutCondition_shouldUseGenericMobilityRequest() throws Exception {
        classifyAs("RECOMMEND_EQUIPMENT", " ");
        EquipmentRecommendResponse body = EquipmentRecommendResponse.builder().condition("General Mobility Issues").build();
        ResponseEntity<?> responseEntity = ResponseEntity.ok(body);
        doReturn(responseEntity).when(equipmentAIController).recommendEquipment(any());

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("recommend equipment"));

        assertEquals("INFO", response.getType());
        assertSame(body, response.getData());
    }

    @Test
    void processCommand_videoCommandWithBlankTopic_shouldUseDefaultTopic() throws Exception {
        classifyAs("VIDEO", " ");
        VoiceCommandRequest request = command("create video");
        request.setPatientName("Amina");
        VideoGenerateResponse video = VideoGenerateResponse.builder().topic("Childhood Memories").build();
        when(videoScriptService.generateVideoScript(any())).thenReturn(video);

        VoiceCommandResponse response = voiceAssistantService.processCommand(request);

        assertEquals("ACTION", response.getType());
        assertSame(video, response.getData());
        assertTrue(response.getMessage().contains("Childhood Memories"));
    }

    @Test
    void processCommand_videoCommandWhenGenerationFails_shouldReturnError() throws Exception {
        classifyAs("VIDEO", "family");
        when(videoScriptService.generateVideoScript(any())).thenThrow(new RuntimeException("renderer offline"));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("create video"));

        assertEquals("ERROR", response.getType());
        assertTrue(response.getMessage().contains("Unable to generate the video"));
    }

    @Test
    void processCommand_statusCommandWithPartialFailures_shouldStillReturnAvailableSections() throws Exception {
        classifyAs("STATUS", "");
        when(gameServiceClient.getQuizCountByCaregiver(10L)).thenThrow(new RuntimeException("game offline"));
        when(medicalServiceClient.getActiveLoansByBorrower(10L)).thenReturn(List.of(
                EquipmentLoanDTO.builder().equipmentId(44L).equipmentName(null).dueDate(null).build(),
                EquipmentLoanDTO.builder().equipmentName("Walker").dueDate(LocalDateTime.of(2026, 5, 12, 8, 0)).build()
        ));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("status"));

        assertEquals("INFO", response.getType());
        assertTrue(response.getMessage().contains("Quiz: data not available"));
        assertTrue(response.getMessage().contains("Equipment #44"));
        assertTrue(response.getMessage().contains("N/A"));
    }

    @Test
    void processCommand_unknownIntentWhenFallbackAiFails_shouldReturnBuiltInHelp() throws Exception {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("not-json").thenThrow(new RuntimeException("AI offline"));
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new RuntimeException("bad json"));

        VoiceCommandResponse response = voiceAssistantService.processCommand(command("what can you do"));

        assertEquals("INFO", response.getType());
        assertTrue(response.getMessage().contains("available commands"));
        assertTrue(response.getMessage().contains("borrow [name]"));
    }

    private VoiceCommandRequest command(String command) {
        return VoiceCommandRequest.builder()
                .command(command)
                .userId(10L)
                .sessionId("session-123")
                .build();
    }

    private void classifyAs(String action, String parameter) throws Exception {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("{\"action\":\"" + action + "\",\"parameter\":\"" + parameter + "\"}");
        when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(Map.of("action", action, "parameter", parameter));
    }
}
