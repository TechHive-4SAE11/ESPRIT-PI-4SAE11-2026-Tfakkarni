package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.ReportAnalysisResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportAnalysisServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private ReportAnalysisService reportAnalysisService;

    @BeforeEach
    void setUp() {
        reportAnalysisService = new ReportAnalysisService(chatClientBuilder, new ObjectMapper());
    }

    @Test
    void analyzeMedicalFolder_shouldBuildPromptAndParseJsonFenceResponse() {
        MedicalFolderDTO folder = sampleFolder();
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(contains("Diagnostic: Early dementia"))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("""
                ```json
                {
                  "cognitiveLevel": "INTERMEDIAIRE",
                  "weakTopics": ["orientation", "short term memory"],
                  "recommendedTopics": ["family photos", "daily routine"],
                  "difficultyLevel": 2,
                  "customPrompt": "Use familiar Tunisian home objects",
                  "diagnosis": "Early dementia"
                }
                ```
                """);

        ReportAnalysisResult result = reportAnalysisService.analyzeMedicalFolder(folder);

        assertEquals("INTERMEDIAIRE", result.getCognitiveLevel());
        assertEquals(List.of("orientation", "short term memory"), result.getWeakTopics());
        assertEquals(List.of("family photos", "daily routine"), result.getRecommendedTopics());
        assertEquals(2, result.getDifficultyLevel());
        assertEquals("Use familiar Tunisian home objects", result.getCustomPrompt());
        assertEquals("Early dementia", result.getDiagnosis());
    }

    @Test
    void analyzeMedicalFolder_whenAiFails_shouldThrowConnectionRuntimeException() {
        when(chatClientBuilder.build()).thenThrow(new RuntimeException("provider offline"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportAnalysisService.analyzeMedicalFolder(sampleFolder()));

        assertEquals("Erreur de connexion avec OpenAI: provider offline", exception.getMessage());
    }

    @Test
    void analyzeMedicalFolder_whenAiReturnsInvalidJson_shouldThrowFormattingRuntimeException() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(org.mockito.ArgumentMatchers.anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("not-json");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reportAnalysisService.analyzeMedicalFolder(sampleFolder()));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().startsWith("Erreur de connexion avec OpenAI: OpenAI formatting error:"));
    }

    private MedicalFolderDTO sampleFolder() {
        MedicalFolderDTO folder = new MedicalFolderDTO();
        folder.setId(15L);
        folder.setPatientId("patient-keycloak-123");
        folder.setDiagnosis("Early dementia");
        folder.setTreatments("Cognitive therapy twice weekly");
        folder.setEvolution("Stable with mild disorientation");
        folder.setWeakPoints(List.of("orientation", "short term memory"));
        folder.setRecommendations("Use visual reminders and familiar routines");
        return folder;
    }
}
