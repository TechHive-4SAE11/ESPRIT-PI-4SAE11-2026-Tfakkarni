package org.techhive.assistantservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.QuizGenerateRequest;
import org.techhive.assistantservice.service.QuizAIService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/quiz")
@RequiredArgsConstructor
public class QuizAIController {

    private final QuizAIService quizAIService;
    private final org.techhive.assistantservice.service.PatientLookupService patientLookupService;
    private final org.techhive.assistantservice.client.MedicalServiceClient medicalServiceClient;
    private final org.techhive.assistantservice.service.ReportAnalysisService reportAnalysisService;

    /**
     * POST /api/ai/quiz/generate
     * Generate a complete quiz using AI (OpenAI GPT).
     * The quiz is saved to game-service via Feign.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(@Valid @RequestBody QuizGenerateRequest request) {
        log.info("AI Quiz generation request: topic={}, questions={}, difficulty={}, caregiver={}",
                request.getTopic(), request.getNumberOfQuestions(),
                request.getDifficultyLevel(), request.getCaregiverId());

        try {
            QuizDTO generatedQuiz = quizAIService.generateQuiz(request);
            return new ResponseEntity<>(generatedQuiz, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Quiz generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Quiz generation failed",
                            "message", e.getMessage()
                    ));
        }
    }

    /**
     * POST /api/ai/quiz/generate-from-patient-name
     * Génère un quiz personnalisé à partir du NOM du patient
     */
    @PostMapping("/generate-from-patient-name")
    public ResponseEntity<?> generateQuizFromPatientName(
            @RequestBody org.techhive.assistantservice.dto.ReportBasedQuizByNameRequest request) {

        log.info("=== GENERATE QUIZ FROM PATIENT NAME ===");
        log.info("Patient name: {}", request.getPatientName());

        try {
            // 1. Chercher le patient par son NOM
            org.techhive.assistantservice.dto.PatientDTO patient = patientLookupService.findPatientByName(request.getPatientName());
            log.info("Patient found: ID={}, Name={} {}", patient.getId(), patient.getFirstName(), patient.getLastName());

            // 2. Récupérer son dossier médical
            String patientIdentifier = patient.getKeycloakId() != null ? patient.getKeycloakId() : String.valueOf(patient.getId());

            try {
                String rawResponse = medicalServiceClient.getMedicalFolderRaw(patientIdentifier);
                log.info("RAW RESPONSE: {}", rawResponse);
            } catch (Exception e) {
                log.warn("Could not fetch raw response: ", e.getMessage());
            }

            List<MedicalFolderDTO> medicalFolders = medicalServiceClient.getMedicalFolderByPatient(patientIdentifier);
            if (medicalFolders == null || medicalFolders.isEmpty()) {
                throw new RuntimeException("No medical folder found for patient ID " + patient.getId());
            }
            org.techhive.assistantservice.dto.MedicalFolderDTO medicalFolder = medicalFolders.get(0);
            medicalFolder.setDiagnosis(patient.getDiagnosis());

            // 3. Analyser le rapport médical avec IA
            org.techhive.assistantservice.dto.ReportAnalysisResult analysis = reportAnalysisService.analyzeMedicalFolder(medicalFolder);
            log.info("Analysis result: level={}, weakTopics={}", analysis.getCognitiveLevel(), analysis.getWeakTopics());

            // 4. Déterminer la difficulté (si non fournie)
            Integer finalDifficulty = request.getDifficultyLevel();
            if (finalDifficulty == null) {
                finalDifficulty = analysis.getDifficultyLevel();
            }

            // 5. Construire le sujet du quiz à partir des recommandations
            String topic = (analysis.getRecommendedTopics() == null || analysis.getRecommendedTopics().isEmpty()) ? 
                "mémoire et cognition" : analysis.getRecommendedTopics().get(0);

            // 6. Construire le contexte personnalisé
            String customContext = String.format(
                "Patient: %s %s (âge: %d). Diagnostic: %s. Points faibles: %s. Niveau cognitif: %s.",
                patient.getFirstName(), patient.getLastName(),
                patient.getAge() != null ? patient.getAge() : 0,
                analysis.getDiagnosis(),
                analysis.getWeakTopics() != null ? String.join(", ", analysis.getWeakTopics()) : "Aucun",
                analysis.getCognitiveLevel()
            );

            // 7. Générer le quiz
            QuizGenerateRequest quizRequest = QuizGenerateRequest.builder()
                .topic(topic)
                .numberOfQuestions(request.getNumberOfQuestions())
                .difficultyLevel(finalDifficulty)
                .caregiverId(patient.getId())
                .customContext(customContext)
                .build();

            QuizDTO quiz = quizAIService.generateQuiz(quizRequest);
            
            log.info("Quiz generated: ID={}, topic={}, questions={}", 
                     quiz.getId(), quiz.getTopic(), quiz.getQuestions().size());

            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            log.error("Failed to generate quiz from patient name: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Custom quiz generation failed",
                            "message", e.getMessage()
                    ));
        }
    }
}
