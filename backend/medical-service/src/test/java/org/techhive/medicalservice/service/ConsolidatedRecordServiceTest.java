package org.techhive.medicalservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.medicalservice.client.GameServiceClient;
import org.techhive.medicalservice.client.TrackingServiceClient;
import org.techhive.medicalservice.client.UserServiceClient;
import org.techhive.medicalservice.dto.game.GameAttemptDTO;
import org.techhive.medicalservice.dto.game.GameStatsDTO;
import org.techhive.medicalservice.dto.tracking.IncidentStatsDTO;
import org.techhive.medicalservice.dto.tracking.MedicationComplianceDTO;
import org.techhive.medicalservice.entity.MedicalFolder;
import org.techhive.medicalservice.entity.MedicalHistory;
import org.techhive.medicalservice.exception.ResourceNotFoundException;
import org.techhive.medicalservice.repository.MedicalFolderRepository;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsolidatedRecordServiceTest {

    @Mock
    private MedicalFolderRepository medicalFolderRepository;

    @Mock
    private GameServiceClient gameServiceClient;

    @Mock
    private TrackingServiceClient trackingServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ConsolidatedRecordService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ConsolidatedRecordService();
        inject("medicalFolderRepository", medicalFolderRepository);
        inject("gameServiceClient", gameServiceClient);
        inject("trackingServiceClient", trackingServiceClient);
        inject("userServiceClient", userServiceClient);
    }

    @Test
    void generateConsolidatedPdfThrowsWhenFolderIsMissing() {
        when(medicalFolderRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.generateConsolidatedPdf(404L));

        assertEquals("Medical Folder not found", ex.getMessage());
        verifyNoInteractions(gameServiceClient, trackingServiceClient, userServiceClient);
    }

    @Test
    void generateConsolidatedPdfIncludesAllSuccessfulSections() throws Exception {
        MedicalFolder folder = folderWithHistory();
        when(medicalFolderRepository.findById(10L)).thenReturn(Optional.of(folder));
        when(userServiceClient.getUserByKeycloakId("patient-a"))
                .thenReturn(objectMapper.readTree("{\"firstName\":\"Nour\",\"lastName\":\"Mansouri\"}"));
        when(userServiceClient.getUserByKeycloakId("doctor-a"))
                .thenReturn(objectMapper.readTree("{\"firstName\":\"Selim\",\"lastName\":\"Gharbi\"}"));
        when(gameServiceClient.getPatientGameStats("patient-a")).thenReturn(GameStatsDTO.builder()
                .totalGamesPlayed(8)
                .averageScore(84.25)
                .recentAttempts(List.of(GameAttemptDTO.builder()
                        .gameName("Memory Cards")
                        .score(95)
                        .playedAt(LocalDateTime.of(2026, 5, 2, 9, 30))
                        .build()))
                .build());
        when(trackingServiceClient.getPatientMedicationComplianceByDrug("patient-a"))
                .thenReturn(List.of(MedicationComplianceDTO.builder()
                        .medicationName("Donepezil")
                        .startDate("2026-04-01")
                        .endDate("2026-04-30")
                        .taken(27)
                        .missed(3)
                        .build()));
        when(trackingServiceClient.getPatientIncidentStats("patient-a"))
                .thenReturn(IncidentStatsDTO.builder()
                        .labels(List.of("fall", "wandering"))
                        .values(List.of(1, 2))
                        .build());

        byte[] pdf = service.generateConsolidatedPdf(10L);

        assertValidPdf(pdf);
        verify(gameServiceClient).getPatientGameStats("patient-a");
        verify(trackingServiceClient).getPatientMedicationComplianceByDrug("patient-a");
        verify(trackingServiceClient).getPatientIncidentStats("patient-a");
    }

    @Test
    void generateConsolidatedPdfFallsBackWhenOptionalRemoteDataIsMissing() throws Exception {
        MedicalFolder folder = folderWithoutHistory();
        when(medicalFolderRepository.findById(11L)).thenReturn(Optional.of(folder));
        when(userServiceClient.getUserByKeycloakId("patient-b"))
                .thenReturn(objectMapper.readTree("{\"firstName\":\"\",\"lastName\":\"\"}"));
        when(userServiceClient.getUserByKeycloakId("doctor-b"))
                .thenThrow(new RuntimeException("user-service down"));
        when(gameServiceClient.getPatientGameStats("patient-b")).thenReturn(null);
        when(trackingServiceClient.getPatientMedicationComplianceByDrug("patient-b")).thenReturn(List.of());
        when(trackingServiceClient.getPatientIncidentStats("patient-b"))
                .thenReturn(IncidentStatsDTO.builder().labels(List.of()).values(List.of()).build());

        byte[] pdf = service.generateConsolidatedPdf(11L);

        assertValidPdf(pdf);
        verify(userServiceClient).getUserByKeycloakId("patient-b");
        verify(userServiceClient).getUserByKeycloakId("doctor-b");
    }

    @Test
    void generateConsolidatedPdfWritesFallbackParagraphsWhenGameAndTrackingClientsFail() {
        MedicalFolder folder = folderWithoutHistory();
        when(medicalFolderRepository.findById(12L)).thenReturn(Optional.of(folder));
        when(userServiceClient.getUserByKeycloakId(anyString())).thenReturn(null);
        when(gameServiceClient.getPatientGameStats("patient-b")).thenThrow(new RuntimeException("game unavailable"));
        when(trackingServiceClient.getPatientMedicationComplianceByDrug("patient-b"))
                .thenThrow(new RuntimeException("tracking unavailable"));

        byte[] pdf = service.generateConsolidatedPdf(12L);

        assertValidPdf(pdf);
        verify(trackingServiceClient, never()).getPatientIncidentStats("patient-b");
    }

    private MedicalFolder folderWithHistory() {
        MedicalFolder folder = MedicalFolder.builder()
                .id(10L)
                .patientId("patient-a")
                .doctorId("doctor-a")
                .createdAt(LocalDateTime.of(2026, 5, 1, 10, 0))
                .build();
        MedicalHistory history = MedicalHistory.builder()
                .conditions("Alzheimer early stage")
                .allergies("Penicillin")
                .symptoms("Memory loss")
                .recommendedTreatment("Cognitive training")
                .build();
        folder.setMedicalHistories(List.of(history));
        return folder;
    }

    private MedicalFolder folderWithoutHistory() {
        return MedicalFolder.builder()
                .id(11L)
                .patientId("patient-b")
                .doctorId("doctor-b")
                .createdAt(LocalDateTime.of(2026, 5, 1, 11, 0))
                .medicalHistories(List.of())
                .build();
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field field = ConsolidatedRecordService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, value);
    }

    private static void assertValidPdf(byte[] pdf) {
        assertNotNull(pdf);
        assertTrue(pdf.length > 1000, "generated PDF should contain enough bytes for the report sections");
        assertTrue(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
    }
}
