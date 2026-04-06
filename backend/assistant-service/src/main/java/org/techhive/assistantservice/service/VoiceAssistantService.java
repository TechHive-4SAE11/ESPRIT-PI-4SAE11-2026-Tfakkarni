package org.techhive.assistantservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.techhive.assistantservice.client.GameServiceClient;
import org.techhive.assistantservice.client.MedicalServiceClient;
import org.techhive.assistantservice.client.dto.EquipmentDTO;
import org.techhive.assistantservice.client.dto.EquipmentLoanDTO;
import org.techhive.assistantservice.client.dto.QuizDTO;
import org.techhive.assistantservice.dto.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Voice assistant service handling natural language commands.
 * Supports: borrow, return, quiz generation, status queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceAssistantService {

    private final ChatClient.Builder chatClientBuilder;
    private final GameServiceClient gameServiceClient;
    private final MedicalServiceClient medicalServiceClient;
    private final QuizAIService quizAIService;

    /**
     * Process a voice command and return an appropriate response.
     */
    public VoiceCommandResponse processCommand(VoiceCommandRequest request) {
        String command = request.getCommand().trim().toLowerCase();
        log.info("Processing voice command: '{}' from user: {}", command, request.getUserId());

        try {
            // Pattern matching for known commands
            if (command.startsWith("emprunter ")) {
                return handleBorrowCommand(command, request.getUserId());
            } else if (command.startsWith("rendre ")) {
                return handleReturnCommand(command, request.getUserId());
            } else if (command.startsWith("quiz sur ") || command.startsWith("quiz about ")) {
                return handleQuizCommand(command, request.getUserId());
            } else if (command.equals("statut") || command.equals("status")) {
                return handleStatusCommand(request.getUserId());
            } else {
                // Use AI for unrecognized commands
                return handleAICommand(command, request.getUserId());
            }
        } catch (Exception e) {
            log.error("Error processing voice command '{}': {}", command, e.getMessage());
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Désolé, une erreur s'est produite: " + e.getMessage())
                    .sessionId(request.getSessionId())
                    .build();
        }
    }

    /**
     * Handle "emprunter [equipment]" command.
     */
    private VoiceCommandResponse handleBorrowCommand(String command, Long userId) {
        String equipmentName = command.replaceFirst("emprunter ", "").trim();
        log.info("Borrow request for: '{}'", equipmentName);

        // Search for equipment
        List<EquipmentDTO> results;
        try {
            results = medicalServiceClient.searchEquipment(equipmentName);
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Impossible de contacter le service médical. Réessayez plus tard.")
                    .build();
        }

        if (results.isEmpty()) {
            return VoiceCommandResponse.builder()
                    .type("INFO")
                    .message("Aucun équipement trouvé avec le nom '" + equipmentName + "'.")
                    .build();
        }

        // Find first available
        EquipmentDTO available = results.stream()
                .filter(eq -> "AVAILABLE".equals(eq.getStatus()))
                .findFirst()
                .orElse(null);

        if (available == null) {
            return VoiceCommandResponse.builder()
                    .type("INFO")
                    .message("L'équipement '" + equipmentName + "' n'est pas disponible actuellement.")
                    .build();
        }

        // Create loan
        EquipmentLoanDTO loanDTO = EquipmentLoanDTO.builder()
                .equipmentId(available.getId())
                .borrowerId(userId)
                .loanDate(LocalDateTime.now().toString())
                .dueDate(LocalDateTime.now().plusDays(14).toString())
                .purpose("Emprunt via assistant vocal")
                .status("ACTIVE")
                .build();

        try {
            // Convert string dates to LocalDateTime for the API call
            EquipmentLoanDTO loanRequest = EquipmentLoanDTO.builder()
                    .equipmentId(available.getId())
                    .borrowerId(userId)
                    .loanDate(LocalDateTime.now().toString())
                    .dueDate(LocalDateTime.now().plusDays(14).toString())
                    .purpose("Emprunt via assistant vocal")
                    .status("ACTIVE")
                    .build();
            EquipmentLoanDTO createdLoan = medicalServiceClient.borrowEquipment(loanRequest);

            return VoiceCommandResponse.builder()
                    .type("ACTION")
                    .message(String.format("✅ Équipement '%s' emprunté avec succès ! Retour prévu dans 14 jours.",
                            available.getName()))
                    .data(createdLoan)
                    .build();
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Échec de l'emprunt: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Handle "rendre [equipment]" command.
     */
    private VoiceCommandResponse handleReturnCommand(String command, Long userId) {
        String equipmentName = command.replaceFirst("rendre ", "").trim();
        log.info("Return request for: '{}'", equipmentName);

        // Find active loans for user
        List<EquipmentLoanDTO> activeLoans;
        try {
            activeLoans = medicalServiceClient.getActiveLoansByBorrower(userId);
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Impossible de récupérer vos emprunts actifs.")
                    .build();
        }

        if (activeLoans.isEmpty()) {
            return VoiceCommandResponse.builder()
                    .type("INFO")
                    .message("Vous n'avez aucun emprunt actif à retourner.")
                    .build();
        }

        // Find matching loan
        EquipmentLoanDTO matchingLoan = activeLoans.stream()
                .filter(loan -> loan.getEquipmentName() != null &&
                        loan.getEquipmentName().toLowerCase().contains(equipmentName.toLowerCase()))
                .findFirst()
                .orElse(null);

        if (matchingLoan == null) {
            String loansList = activeLoans.stream()
                    .map(l -> "- " + (l.getEquipmentName() != null ? l.getEquipmentName() : "ID:" + l.getEquipmentId()))
                    .reduce("", (a, b) -> a + "\n" + b);
            return VoiceCommandResponse.builder()
                    .type("INFO")
                    .message("Aucun emprunt correspondant à '" + equipmentName + "'.\nVos emprunts actifs:" + loansList)
                    .build();
        }

        try {
            EquipmentLoanDTO returnedLoan = medicalServiceClient.returnEquipment(matchingLoan.getId());
            return VoiceCommandResponse.builder()
                    .type("ACTION")
                    .message(String.format("✅ Équipement '%s' retourné avec succès !",
                            matchingLoan.getEquipmentName()))
                    .data(returnedLoan)
                    .build();
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Échec du retour: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Handle "quiz sur [subject]" command.
     */
    private VoiceCommandResponse handleQuizCommand(String command, Long userId) {
        String topic = command.replaceFirst("quiz (sur|about) ", "").trim();
        log.info("Quiz generation request on topic: '{}'", topic);

        QuizGenerateRequest quizRequest = QuizGenerateRequest.builder()
                .topic(topic)
                .numberOfQuestions(5)
                .difficultyLevel(1)
                .caregiverId(userId)
                .build();

        try {
            QuizDTO generatedQuiz = quizAIService.generateQuiz(quizRequest);
            return VoiceCommandResponse.builder()
                    .type("QUIZ_START")
                    .message(String.format("🎯 Quiz '%s' créé avec %d questions ! Prêt à commencer ?",
                            topic, generatedQuiz.getQuestions() != null ? generatedQuiz.getQuestions().size() : 0))
                    .data(generatedQuiz)
                    .build();
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Impossible de générer le quiz: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Handle "statut" command - summarize quiz scores + active loans.
     */
    private VoiceCommandResponse handleStatusCommand(Long userId) {
        log.info("Status request for user: {}", userId);

        StringBuilder status = new StringBuilder("📊 **Votre statut:**\n\n");

        // Quiz stats
        try {
            Long quizCount = gameServiceClient.getQuizCountByCaregiver(userId);
            Double avgScore = gameServiceClient.getAverageScoreByCaregiver(userId);
            status.append(String.format("🧠 **Quiz:** %d quiz effectués, score moyen: %.1f%%\n",
                    quizCount != null ? quizCount : 0,
                    avgScore != null ? avgScore : 0.0));

            List<String> weakTopics = gameServiceClient.getWeakTopicsByCaregiver(userId);
            if (weakTopics != null && !weakTopics.isEmpty()) {
                status.append("⚠️ Sujets à améliorer: ").append(String.join(", ", weakTopics)).append("\n");
            }
        } catch (Exception e) {
            status.append("🧠 Quiz: données non disponibles\n");
        }

        status.append("\n");

        // Loan stats
        try {
            List<EquipmentLoanDTO> activeLoans = medicalServiceClient.getActiveLoansByBorrower(userId);
            status.append(String.format("🏥 **Emprunts actifs:** %d\n", activeLoans.size()));
            for (EquipmentLoanDTO loan : activeLoans) {
                status.append(String.format("  - %s (retour: %s)\n",
                        loan.getEquipmentName() != null ? loan.getEquipmentName() : "Équipement #" + loan.getEquipmentId(),
                        loan.getDueDate() != null ? loan.getDueDate() : "N/A"));
            }
        } catch (Exception e) {
            status.append("🏥 Emprunts: données non disponibles\n");
        }

        return VoiceCommandResponse.builder()
                .type("INFO")
                .message(status.toString())
                .build();
    }

    /**
     * Handle unrecognized commands using AI natural language understanding.
     */
    private VoiceCommandResponse handleAICommand(String command, Long userId) {
        log.info("Using AI to interpret command: '{}'", command);

        String prompt = String.format("""
                You are a helpful voice assistant for a medical memory care application (Tfakkarni).
                The user said: "%s"
                
                Available actions:
                1. "emprunter [nom]" - Borrow medical equipment
                2. "rendre [nom]" - Return medical equipment
                3. "quiz sur [sujet]" - Generate a memory quiz on a topic
                4. "statut" - Show quiz scores and active equipment loans
                
                If the command matches one of these actions, suggest the correct command format.
                Otherwise, provide a helpful response in French.
                
                Respond with a brief, friendly message in French.
                """, command);

        ChatClient chatClient = chatClientBuilder.build();
        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return VoiceCommandResponse.builder()
                .type("INFO")
                .message(aiResponse)
                .build();
    }
}
