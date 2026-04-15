package org.techhive.mlservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.mlservice.client.MedicalServiceClient;
import org.techhive.mlservice.dto.AppointmentResponseDTO;
import org.techhive.mlservice.repository.ComplianceHistoryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock
    private MedicalServiceClient medicalServiceClient;

    @Mock
    private ComplianceHistoryRepository complianceHistoryRepository;

    @InjectMocks
    private ComplianceService complianceService;

    @Test
    void testCalculateCompliance_AllConfirmed_ShouldReturnHighScore() {
        // Arrange
        AppointmentResponseDTO apt1 = new AppointmentResponseDTO();
        apt1.setStatus("CONFIRMED");
        apt1.setStartTime(LocalDateTime.now());

        AppointmentResponseDTO apt2 = new AppointmentResponseDTO();
        apt2.setStatus("CONFIRMED");
        apt2.setStartTime(LocalDateTime.now());

        when(medicalServiceClient.getAppointments("1")).thenReturn(List.of(apt1, apt2));

        // Act
        Map<String, Object> result = complianceService.calculateCompliance("1");

        // Assert
        assertEquals(100.0, result.get("score"));
        assertEquals("INFO", result.get("severity"));
        assertEquals("Message encouragement", result.get("message"));
        verify(complianceHistoryRepository, times(1)).save(any());
    }

    @Test
    void testCalculateCompliance_WithMissedAppointments_ShouldReturnLowerScore() {
        // Arrange
        AppointmentResponseDTO apt1 = new AppointmentResponseDTO();
        apt1.setStatus("MISSED");
        apt1.setStartTime(LocalDateTime.now());

        AppointmentResponseDTO apt2 = new AppointmentResponseDTO();
        apt2.setStatus("CONFIRMED");
        apt2.setStartTime(LocalDateTime.now());

        when(medicalServiceClient.getAppointments("1")).thenReturn(List.of(apt1, apt2));

        // Act
        Map<String, Object> result = complianceService.calculateCompliance("1");

        // Assert
        assertEquals(80.0, result.get("score"));
    }

    @Test
    void testCalculateCompliance_WithCancelledAppointments_ShouldReturnLowerScore() {
        // Arrange
        AppointmentResponseDTO apt1 = new AppointmentResponseDTO();
        apt1.setStatus("CANCELLED");
        apt1.setStartTime(LocalDateTime.now());

        AppointmentResponseDTO apt2 = new AppointmentResponseDTO();
        apt2.setStatus("CONFIRMED");
        apt2.setStartTime(LocalDateTime.now());

        when(medicalServiceClient.getAppointments("1")).thenReturn(List.of(apt1, apt2));

        // Act
        Map<String, Object> result = complianceService.calculateCompliance("1");

        // Assert
        assertEquals(80.0, result.get("score"));
    }

    @Test
    void testCalculateCompliance_ScoreBelow30_ShouldReturnCritique() {
        // Arrange
        AppointmentResponseDTO apt1 = new AppointmentResponseDTO();
        apt1.setStatus("MISSED");
        apt1.setStartTime(LocalDateTime.now());

        AppointmentResponseDTO apt2 = new AppointmentResponseDTO();
        apt2.setStatus("MISSED");
        apt2.setStartTime(LocalDateTime.now());

        AppointmentResponseDTO apt3 = new AppointmentResponseDTO();
        apt3.setStatus("MISSED");
        apt3.setStartTime(LocalDateTime.now());

        AppointmentResponseDTO apt4 = new AppointmentResponseDTO();
        apt4.setStatus("MISSED");
        apt4.setStartTime(LocalDateTime.now());

        when(medicalServiceClient.getAppointments("1")).thenReturn(List.of(apt1, apt2, apt3, apt4));

        // Act
        Map<String, Object> result = complianceService.calculateCompliance("1");

        // Assert
        assertEquals(20.0, result.get("score"));
        assertEquals("CRITIQUE", result.get("severity"));
        assertEquals("Alerte critique : mauvaise observance", result.get("message"));
    }

    @Test
    void testCalculateCompliance_ScoreBetween30And50_ShouldReturnModeree() {
        // Arrange
        AppointmentResponseDTO apt1 = new AppointmentResponseDTO();
        apt1.setStatus("MISSED");
        apt1.setStartTime(LocalDateTime.now());

        AppointmentResponseDTO apt2 = new AppointmentResponseDTO();
        apt2.setStatus("CONFIRMED");
        apt2.setStartTime(LocalDateTime.now());

        when(medicalServiceClient.getAppointments("1")).thenReturn(List.of(apt1, apt2));

        // Act
        Map<String, Object> result = complianceService.calculateCompliance("1");

        // Assert
        assertEquals(80.0, result.get("score"));
        assertEquals("INFO", result.get("severity"));
    }

    @Test
    void testCalculateCompliance_EmptyAppointments_ShouldReturnDefaultScore() {
        // Arrange
        when(medicalServiceClient.getAppointments("1")).thenReturn(List.of());

        // Act
        Map<String, Object> result = complianceService.calculateCompliance("1");

        // Assert
        assertEquals(100.0, result.get("score"));
        assertEquals("INFO", result.get("severity"));
        assertEquals("Aucun rendez-vous trouvé.", result.get("message"));
    }

    @Test
    void testCalculateCompliance_NullAppointments_ShouldReturnDefaultScore() {
        // Arrange
        when(medicalServiceClient.getAppointments("1")).thenReturn(null);

        // Act
        Map<String, Object> result = complianceService.calculateCompliance("1");

        // Assert
        assertEquals(100.0, result.get("score"));
    }
}
