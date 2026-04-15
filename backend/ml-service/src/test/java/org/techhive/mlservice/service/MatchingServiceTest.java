package org.techhive.mlservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.mlservice.client.MedicalServiceClient;
import org.techhive.mlservice.dto.MedicalFolderResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private MedicalServiceClient medicalServiceClient;

    @InjectMocks
    private MatchingService matchingService;

    @Test
    void testGetMatching_WithMemoryLoss_ShouldReturnNeurology() {
        // Arrange
        MedicalFolderResponse folder = new MedicalFolderResponse();
        folder.setSymptomes("perte de mémoire, confusion");
        when(medicalServiceClient.getMedicalFolder("1")).thenReturn(List.of(folder));

        // Act
        Map<String, String> result = matchingService.getMatching("1");

        // Assert
        assertEquals("Neurologie", result.get("specialty"));
        assertTrue(result.get("message").contains("mémoire"));
        verify(medicalServiceClient, times(1)).getMedicalFolder("1");
    }

    @Test
    void testGetMatching_WithCardiacSymptoms_ShouldReturnCardiologie() {
        // Arrange
        MedicalFolderResponse folder = new MedicalFolderResponse();
        folder.setSymptomes("essoufflement");
        folder.setAntecedents("cardiaque");
        when(medicalServiceClient.getMedicalFolder("1")).thenReturn(List.of(folder));

        // Act
        Map<String, String> result = matchingService.getMatching("1");

        // Assert
        assertEquals("Cardiologie", result.get("specialty"));
        assertTrue(result.get("message").contains("cardiaque"));
    }

    @Test
    void testGetMatching_WithAlzheimerKeyword_ShouldReturnNeurology() {
        // Arrange
        MedicalFolderResponse folder = new MedicalFolderResponse();
        folder.setSymptomes("alzheimer débutant");
        when(medicalServiceClient.getMedicalFolder("1")).thenReturn(List.of(folder));

        // Act
        Map<String, String> result = matchingService.getMatching("1");

        // Assert
        assertEquals("Neurologie", result.get("specialty"));
    }

    @Test
    void testGetMatching_EmptyFolder_ShouldReturnGeneraliste() {
        // Arrange
        when(medicalServiceClient.getMedicalFolder("1")).thenReturn(List.of());

        // Act
        Map<String, String> result = matchingService.getMatching("1");

        // Assert
        assertEquals("Généraliste", result.get("specialty"));
        assertEquals("Aucun dossier trouvé, médecin généraliste recommandé.", result.get("message"));
    }

    @Test
    void testGetMatching_NullFolder_ShouldReturnGeneraliste() {
        // Arrange
        when(medicalServiceClient.getMedicalFolder("1")).thenReturn(null);

        // Act
        Map<String, String> result = matchingService.getMatching("1");

        // Assert
        assertEquals("Généraliste", result.get("specialty"));
    }

    @Test
    void testGetMatching_NoSpecificSymptoms_ShouldReturnGeneraliste() {
        // Arrange
        MedicalFolderResponse folder = new MedicalFolderResponse();
        folder.setAllergies("aucune");
        folder.setAntecedents("rien");
        folder.setSymptomes("fatigue générale");
        when(medicalServiceClient.getMedicalFolder("1")).thenReturn(List.of(folder));

        // Act
        Map<String, String> result = matchingService.getMatching("1");

        // Assert
        assertEquals("Généraliste", result.get("specialty"));
        assertEquals("Consultation de suivi standard.", result.get("message"));
    }
}
