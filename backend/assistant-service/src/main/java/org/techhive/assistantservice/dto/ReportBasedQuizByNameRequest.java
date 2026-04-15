package org.techhive.assistantservice.dto;

import lombok.Data;

@Data
public class ReportBasedQuizByNameRequest {
    private String patientName;        // Nom complet du patient (ex: "Jean Dupont")
    private Integer numberOfQuestions; // Nombre de questions (3, 5, 10)
    private Integer difficultyLevel;   // Optionnel: 1 (Facile), 2 (Moyen), 3 (Difficile)
    private Long caregiverId;          // ID du caregiver qui génère le quiz
}
