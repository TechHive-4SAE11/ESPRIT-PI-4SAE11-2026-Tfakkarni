package org.techhive.assistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAnalysisResult {
    private String cognitiveLevel;      // DEBUTANT, INTERMEDIAIRE, AVANCE
    private List<String> weakTopics;    // Sujets où le patient a des difficultés
    private List<String> recommendedTopics; // Sujets recommandés
    private Integer difficultyLevel;    // 1, 2 ou 3
    private String customPrompt;        // Description personnalisée du patient
    private String diagnosis;           // Diagnostic principal
}
