package org.techhive.mlservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.mlservice.dto.ModuleDTO;
import org.techhive.mlservice.entity.TrainingModule;
import org.techhive.mlservice.entity.UserProgress;
import org.techhive.mlservice.repository.TrainingModuleRepository;
import org.techhive.mlservice.repository.UserProgressRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock
    private TrainingModuleRepository trainingModuleRepository;

    @Mock
    private UserProgressRepository userProgressRepository;

    @InjectMocks
    private TrainingServiceImpl trainingService;

    @Test
    void testGetModules_ShouldReturnListOfModules() {
        // Arrange
        TrainingModule module = new TrainingModule();
        module.setId(1L);
        module.setTitle("Test Module");
        module.setActive(true);

        when(trainingModuleRepository.findByActiveTrue()).thenReturn(List.of(module));

        // Act
        List<ModuleDTO> result = trainingService.getModules();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Test Module", result.get(0).getTitle());
    }

    @Test
    void testGetModuleById_ExistingId_ShouldReturnModule() {
        // Arrange
        TrainingModule module = new TrainingModule();
        module.setId(1L);
        module.setTitle("Test Module");

        when(trainingModuleRepository.findById(1L)).thenReturn(Optional.of(module));

        // Act
        ModuleDTO result = trainingService.getModuleById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Module", result.getTitle());
    }

    @Test
    void testGetModuleById_NonExistingId_ShouldThrowException() {
        // Arrange
        when(trainingModuleRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> trainingService.getModuleById(99L));
    }

    @Test
    void testGetUserProgress_NoProgress_ShouldReturnZero() {
        // Arrange
        when(userProgressRepository.findByUserId(1L)).thenReturn(List.of());

        // Act
        var result = trainingService.getUserProgress(1L);

        // Assert
        assertEquals(0, result.getCompletedModules());
        assertEquals(0, result.getPercentage());
    }

    @Test
    void testGetUserProgress_WithProgress_ShouldCalculateCorrectly() {
        // Arrange
        UserProgress progress = new UserProgress();
        progress.setCompleted(true);

        when(userProgressRepository.findByUserId(1L)).thenReturn(List.of(progress));
        when(trainingModuleRepository.count()).thenReturn(4L);

        // Act
        var result = trainingService.getUserProgress(1L);

        // Assert
        assertEquals(1, result.getCompletedModules());
        assertEquals(4, result.getTotalModules());
        assertEquals(25.0, result.getPercentage());
    }
}