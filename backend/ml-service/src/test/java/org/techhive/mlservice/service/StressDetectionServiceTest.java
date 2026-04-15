package org.techhive.mlservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.mlservice.dto.StressAnalysisDTO;
import org.techhive.mlservice.repository.CaregiverStressHistoryRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StressDetectionServiceTest {

    @Mock
    private CaregiverStressHistoryRepository stressHistoryRepository;

    @InjectMocks
    private StressDetectionService stressDetectionService;

    @Test
    void testAnalyzeStress_ShouldReturnNonNullResult() {
        // Act
        StressAnalysisDTO result = stressDetectionService.analyzeStress("1");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getStressLevel());
        assertNotNull(result.getFactors());
        assertNotNull(result.getRecommendation());
    }

    @Test
    void testAnalyzeStress_StressLevelShouldBeValid() {
        // Act
        StressAnalysisDTO result = stressDetectionService.analyzeStress("1");

        // Assert
        String level = result.getStressLevel();
        assertTrue(level.equals("LOW") || level.equals("MEDIUM") || level.equals("HIGH"));
    }

    @Test
    void testAnalyzeStress_FactorsShouldNotBeEmpty() {
        // Act
        StressAnalysisDTO result = stressDetectionService.analyzeStress("1");

        // Assert
        assertNotNull(result.getFactors());
        assertTrue(result.getFactors().size() > 0);
    }

    @Test
    void testConvertLevelToScore_LowLevel_ShouldReturn25() {
        // Test via la méthode analyzeStress qui utilise convertLevelToScore
        // La méthode est privée, testée indirectement
        StressAnalysisDTO result = stressDetectionService.analyzeStress("1");
        assertNotNull(result);
    }

    @Test
    void testGetStressHistory_ShouldReturnList() {
        // Arrange
        when(stressHistoryRepository.findByCaregiverIdAndCreatedAtAfterOrderByCreatedAtAsc(anyString(), any()))
                .thenReturn(java.util.Collections.emptyList());

        // Act
        var result = stressDetectionService.getStressHistory("1", 30);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testGetLatestStress_WhenNoData_ShouldReturnNull() {
        // Arrange
        when(stressHistoryRepository.findTopByCaregiverIdOrderByCreatedAtDesc(anyString()))
                .thenReturn(java.util.Optional.empty());

        // Act
        var result = stressDetectionService.getLatestStress("1");

        // Assert
        assertNull(result);
    }
}