package org.techhive.assistantservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.dto.AIReportDTO;
import org.techhive.assistantservice.dto.MedicalFolderDTO;
import org.techhive.assistantservice.dto.PatientDTO;
import org.techhive.assistantservice.dto.QuizGenerateRequest;
import org.techhive.assistantservice.dto.ReportBasedQuizRequest;
import org.techhive.assistantservice.service.PatientLookupService;
import org.techhive.assistantservice.service.QuizAIService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai/quiz")
@RequiredArgsConstructor
public class QuizFromReportController {

    private final PatientLookupService patientLookupService;
    private final MedicalServiceClient medicalServiceClient;
    private final QuizAIService quizAIService;

    /**
     * POST /api/ai/quiz/generate-from-patient-report
     * Génère un quiz personnalisé à partir de IAReport + MedicalFolder + Patient Cognitive Level
     */
    @PostMapping("/generate-from-patient-report")
    public ResponseEntity<?> generateQuizFromPatientReport(@RequestBody ReportBasedQuizRequest request) {
        log.info("=== GENERATE QUIZ FROM IA REPORT ===");
        log.info("Patient name: {}", request.getPatientName());

        try {
            // 1. Chercher le patient par son NOM
            PatientDTO patient = patientLookupService.findPatientByName(request.getPatientName());
            log.info("Patient found: ID={}, Name={} {}", patient.getId(), patient.getFirstName(), patient.getLastName());

            // On simule le niveau cognitif si non défini
            Integer difficultyLevel = patient.getCognitiveLevel() != null ? patient.getCognitiveLevel() : 2;

            // 2. Récupérer son MedicalFolder
            String patientIdentifier = patient.getKeycloakId() != null ? patient.getKeycloakId() : String.valueOf(patient.getId());
            List<MedicalFolderDTO> medicalFolders = medicalServiceClient.getMedicalFolderByPatient(patientIdentifier);
            if (medicalFolders == null || medicalFolders.isEmpty()) {
                throw new RuntimeException("No medical folder found for patient ID " + patientIdentifier);
            }
            MedicalFolderDTO medicalFolder = medicalFolders.get(0);
            
            // 3. Récupérer son IAReport
            AIReportDTO aiReport = null;
            try {
                aiReport = medicalServiceClient.getLatestAIReport(medicalFolder.getId());
                log.info("IA Report found: ID={}, Status={}", aiReport.getId(), aiReport.getStatus());
            } catch (Exception e) {
                log.warn("No IA Report found for medical folder {}, generating without it.", medicalFolder.getId());
            }

            // 4. Analyser ces rapports avec OpenAI (intégré dans la création du context)
            String topic = "Cognitive evaluation and Memory";
            
            String aiReportJson = (aiReport != null && aiReport.getReportJson() != null) ? aiReport.getReportJson() : "Aucun";
            String customContext = String.format(
                "Patient: %s %s (âge: %d).\nDiagnostic: %s.\nIA Report JSON (Points faibles & Traitements): %s.\nNiveau de difficulté adapté: %d.",
                patient.getFirstName(), patient.getLastName(),
                patient.getAge() != null ? patient.getAge() : 0,
                patient.getDiagnosis() != null ? patient.getDiagnosis() : "Non spécifié",
                aiReportJson,
                difficultyLevel
            );

            log.info("Custom context created for AI: {}", customContext);

            // 5. Générer un quiz avec difficulté = niveau du patient
            QuizGenerateRequest quizRequest = QuizGenerateRequest.builder()
                .topic(topic)
                .numberOfQuestions(request.getNumberOfQuestions() != null ? request.getNumberOfQuestions() : 5)
                .difficultyLevel(difficultyLevel)
                .caregiverId(1L) // Fixé pour l'exemple
                .customContext(customContext)
                .build();

            // 6. Sauvegarder et retourne le quiz
            QuizDTO quiz = quizAIService.generateQuiz(quizRequest);
            
            log.info("Quiz generated and saved: ID={}, questions={}", quiz.getId(), quiz.getQuestions().size());
            return ResponseEntity.ok(quiz);

        } catch (Exception e) {
            log.error("Failed to generate quiz from patient IA report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Custom quiz generation failed", "message", e.getMessage()));
        }
    }
}
