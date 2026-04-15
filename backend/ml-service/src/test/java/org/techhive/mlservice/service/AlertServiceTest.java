package org.techhive.mlservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.mlservice.client.GameServiceClient;
import org.techhive.mlservice.dto.GameStatsResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private GameServiceClient gameServiceClient;

    @InjectMocks
    private AlertService alertService;

    @Test
    void testGetAlerts_ScoreBelow50_ShouldReturnCritical() {
        // Arrange
        GameStatsResponse stats = new GameStatsResponse();
        stats.setAverageScore(45.0);
        when(gameServiceClient.getGameStats("user1")).thenReturn(stats);

        // Act
        Map<String, Object> result = alertService.getAlerts("user1");

        // Assert
        assertEquals("CRITIQUE", result.get("severity"));
        assertEquals(45.0, result.get("score"));
        assertEquals("Prendre rendez-vous", result.get("action"));
        assertEquals("Score très bas", result.get("message"));
    }

    @Test
    void testGetAlerts_ScoreBetween50And70_ShouldReturnModerate() {
        // Arrange
        GameStatsResponse stats = new GameStatsResponse();
        stats.setAverageScore(65.0);
        when(gameServiceClient.getGameStats("user1")).thenReturn(stats);

        // Act
        Map<String, Object> result = alertService.getAlerts("user1");

        // Assert
        assertEquals("MODEREE", result.get("severity"));
        assertEquals(65.0, result.get("score"));
        assertEquals("Planifier suivi", result.get("action"));
        assertEquals("Score moyen", result.get("message"));
    }

    @Test
    void testGetAlerts_ScoreExactly50_ShouldReturnModerate() {
        // Arrange
        GameStatsResponse stats = new GameStatsResponse();
        stats.setAverageScore(50.0);
        when(gameServiceClient.getGameStats("user1")).thenReturn(stats);

        // Act
        Map<String, Object> result = alertService.getAlerts("user1");

        // Assert
        assertEquals("MODEREE", result.get("severity"));
        assertEquals("Planifier suivi", result.get("action"));
    }

    @Test
    void testGetAlerts_ScoreExactly70_ShouldReturnModerate() {
        // Arrange
        GameStatsResponse stats = new GameStatsResponse();
        stats.setAverageScore(70.0);
        when(gameServiceClient.getGameStats("user1")).thenReturn(stats);

        // Act
        Map<String, Object> result = alertService.getAlerts("user1");

        // Assert
        assertEquals("MODEREE", result.get("severity"));
    }

    @Test
    void testGetAlerts_ScoreAbove70_ShouldReturnInfo() {
        // Arrange
        GameStatsResponse stats = new GameStatsResponse();
        stats.setAverageScore(85.0);
        when(gameServiceClient.getGameStats("user1")).thenReturn(stats);

        // Act
        Map<String, Object> result = alertService.getAlerts("user1");

        // Assert
        assertEquals("INFO", result.get("severity"));
        assertNull(result.get("action"));
        assertEquals("Score normal", result.get("message"));
    }

    @Test
    void testGetAlerts_NullStats_ShouldReturnInfo() {
        // Arrange
        when(gameServiceClient.getGameStats("user1")).thenReturn(null);

        // Act
        Map<String, Object> result = alertService.getAlerts("user1");

        // Assert
        assertEquals("INFO", result.get("severity"));
        assertEquals("Aucune donnée", result.get("message"));
    }
}
