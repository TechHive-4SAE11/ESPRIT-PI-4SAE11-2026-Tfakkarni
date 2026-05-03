package org.techhive.assistantservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.client.dto.EquipmentDTO;
import org.techhive.assistantservice.client.dto.EquipmentLoanDTO;
import org.techhive.assistantservice.dto.EquipmentRecommendRequest;
import org.techhive.assistantservice.dto.EquipmentRecommendResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentAIServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private MedicalServiceClient medicalServiceClient;

    private EquipmentAIService equipmentAIService;

    @BeforeEach
    void setUp() {
        equipmentAIService = new EquipmentAIService(chatClientBuilder, medicalServiceClient, new ObjectMapper());
    }

    @Test
    void recommendEquipment_withInventoryRecommendation_shouldBorrowExistingEquipment() {
        EquipmentDTO walker = EquipmentDTO.builder()
                .id(7L)
                .name("Adjustable Walker")
                .category("MOBILITY")
                .description("Stable support")
                .condition("GOOD")
                .build();
        when(medicalServiceClient.getAvailableEquipment()).thenReturn(List.of(walker));
        mockAiResponse("""
                ```json
                {
                  "recommendations": [
                    {
                      "equipmentId": 7,
                      "equipmentName": "Adjustable Walker",
                      "category": "MOBILITY",
                      "justification": "Improves balance during daily movement",
                      "relevanceScore": 0.91,
                      "usageInstructions": "Use on flat surfaces"
                    }
                  ],
                  "generalAdvice": "Supervised mobility exercises are recommended."
                }
                ```
                """);

        EquipmentRecommendResponse response = equipmentAIService.recommendEquipment(sampleRequest("MOBILITY", "MODERATE"));

        assertEquals(99L, response.getPatientId());
        assertEquals("MOBILITY", response.getCondition());
        assertEquals("MODERATE", response.getSeverity());
        assertEquals("Supervised mobility exercises are recommended.", response.getGeneralAdvice());
        assertEquals(1, response.getRecommendations().size());
        assertEquals(7L, response.getRecommendations().get(0).getEquipmentId());
        verify(medicalServiceClient, never()).createEquipment(any(EquipmentDTO.class));
        verify(medicalServiceClient).borrowEquipment(any(EquipmentLoanDTO.class));
    }

    @Test
    void recommendEquipment_whenAiInventsEquipment_shouldCreateAndBorrowNewEquipment() {
        when(medicalServiceClient.getAvailableEquipment()).thenReturn(List.of());
        mockAiResponse("""
                {
                  "recommendations": [
                    {
                      "equipmentId": -1,
                      "equipmentName": "Connected Fall Detector",
                      "category": "MONITORING",
                      "justification": "Alerts caregivers after falls",
                      "relevanceScore": "0.87",
                      "usageInstructions": "Wear daily"
                    }
                  ],
                  "generalAdvice": "Keep emergency contacts updated."
                }
                """);
        when(medicalServiceClient.createEquipment(any(EquipmentDTO.class)))
                .thenReturn(EquipmentDTO.builder().id(42L).name("Connected Fall Detector").build());

        EquipmentRecommendResponse response = equipmentAIService.recommendEquipment(sampleRequest("ALZHEIMER", "SEVERE"));

        assertEquals(-1L, response.getRecommendations().get(0).getEquipmentId());
        assertEquals(0.87, response.getRecommendations().get(0).getRelevanceScore());
        verify(medicalServiceClient).createEquipment(any(EquipmentDTO.class));
        verify(medicalServiceClient).borrowEquipment(any(EquipmentLoanDTO.class));
    }

    @Test
    void recommendEquipment_whenMedicalServiceAndAiFail_shouldUseFallbackRecommendations() {
        when(medicalServiceClient.getAvailableEquipment()).thenThrow(new RuntimeException("medical unavailable"));
        when(chatClientBuilder.build()).thenThrow(new RuntimeException("openai unavailable"));
        when(medicalServiceClient.createEquipment(any(EquipmentDTO.class)))
                .thenReturn(EquipmentDTO.builder().id(11L).name("Portable Oxygen Concentrator").build())
                .thenReturn(EquipmentDTO.builder().id(12L).name("Ultrasonic Nebulizer").build())
                .thenReturn(EquipmentDTO.builder().id(13L).name("Pulse Oximeter").build());

        EquipmentRecommendResponse response = equipmentAIService.recommendEquipment(sampleRequest("RESPIRATORY", "HIGH"));

        assertNotNull(response.getRecommendations());
        assertEquals(3, response.getRecommendations().size());
        assertTrue(response.getGeneralAdvice().contains("respiratory condition of HIGH severity"));
        verify(medicalServiceClient).getAvailableEquipment();
        verify(medicalServiceClient, org.mockito.Mockito.times(3)).createEquipment(any(EquipmentDTO.class));
        verify(medicalServiceClient, org.mockito.Mockito.times(3)).borrowEquipment(any(EquipmentLoanDTO.class));
    }

    @Test
    void recommendEquipment_whenAiReturnsInvalidJson_shouldReturnEmptyRecommendationResponse() {
        when(medicalServiceClient.getAvailableEquipment()).thenReturn(List.of());
        mockAiResponse("not-json");

        EquipmentRecommendResponse response = equipmentAIService.recommendEquipment(sampleRequest("UNKNOWN", "LOW"));

        assertEquals(99L, response.getPatientId());
        assertEquals("UNKNOWN", response.getCondition());
        assertEquals("LOW", response.getSeverity());
        assertNotNull(response.getRecommendations());
        assertTrue(response.getRecommendations().isEmpty());
        assertEquals("Unable to generate AI recommendations. Please consult your healthcare provider.", response.getGeneralAdvice());
        verify(medicalServiceClient, never()).borrowEquipment(any(EquipmentLoanDTO.class));
    }

    private EquipmentRecommendRequest sampleRequest(String condition, String severity) {
        return EquipmentRecommendRequest.builder()
                .patientId(99L)
                .condition(condition)
                .severity(severity)
                .customContext("Lives with caregiver and needs safe home support")
                .build();
    }

    private void mockAiResponse(String content) {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(content);
    }
}
