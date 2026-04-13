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
import java.util.Map;
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
    private final VideoScriptService videoScriptService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * Process a voice command and return an appropriate response.
     */
    public VoiceCommandResponse processCommand(VoiceCommandRequest request) {
        String command = request.getCommand().trim();
        log.info("Processing voice command: '{}' from user: {}", command, request.getUserId());

        try {
            // STEP 1: Use Spring AI to classify the user's intent
            String aiClassification = classifyIntentWithAI(command);
            log.info("AI Classification Result: {}", aiClassification);

            Map<String, String> parsed = parseAIClassification(aiClassification);
            String intent = parsed.getOrDefault("action", "UNKNOWN");
            String parameter = parsed.getOrDefault("parameter", "");

            // STEP 2: Route to the proper handler based on the exact intent
            return switch (intent.toUpperCase()) {
                case "BORROW" -> handleBorrowCommand(parameter, request.getUserId());
                case "RETURN" -> handleReturnCommand(parameter, request.getUserId());
                case "QUIZ" -> handleQuizCommand(parameter, request.getUserId());
                case "STATUS" -> handleStatusCommand(request.getUserId());
                case "VIDEO" -> handleVideoCommand(parameter, request.getUserId(), request.getPatientName());
                default -> handleAICommand(command, request.getUserId()); // Fallback chat
            };
        } catch (Exception e) {
            log.error("Error processing voice command '{}': {}", command, e.getMessage());
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Désolé, une erreur s'est produite: " + e.getMessage())
                    .sessionId(request.getSessionId())
                    .build();
        }
    }

    private String classifyIntentWithAI(String command) {
        String prompt = String.format("""
                You are an intent classifier for a French medical application voice assistant.
                Classify the following user command into exactly one of these actions:
                - BORROW: Empunter ou demander un équipement médical (fauteuil, lit, etc)
                - RETURN: Rendre, retourner, ou rendre un équipement
                - QUIZ: Générer, créer ou lancer un quiz sur un sujet donné
                - STATUS: Demander son statut, ses scores, ou ses emprunts
                - VIDEO: Créer ou générer une vidéo souvenir ou thérapeutique sur un sujet
                - UNKNOWN: Si cela ne correspond à rien.
                
                Command: "%s"
                
                Respond ONLY with a JSON object containing "action" and "parameter" (the subject/item, empty if none).
                Example:
                {"action": "BORROW", "parameter": "fauteuil roulant"}
                {"action": "QUIZ", "parameter": "la géographie"}
                {"action": "VIDEO", "parameter": "souvenirs de jeunesse"}
                """, command);

        ChatClient chatClient = chatClientBuilder.build();
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseAIClassification(String response) {
        try {
            String cleaned = response.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            else if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            return objectMapper.readValue(cleaned.trim(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Failed to parse intent JSON: {}", response);
            return Map.of("action", "UNKNOWN", "parameter", "");
        }
    }

    /**
     * Handle "emprunter [equipment]" command.
     */
    private VoiceCommandResponse handleBorrowCommand(String equipmentName, Long userId) {
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
                .loanDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(14))
                .purpose("Emprunt via assistant vocal")
                .status("ACTIVE")
                .build();

        try {
            EquipmentLoanDTO createdLoan = medicalServiceClient.borrowEquipment(loanDTO);

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
    private VoiceCommandResponse handleReturnCommand(String equipmentName, Long userId) {
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
    private VoiceCommandResponse handleQuizCommand(String topic, Long userId) {
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
     * Handle video generation command.
     */
    private VoiceCommandResponse handleVideoCommand(String topic, Long userId, String patientName) {
        log.info("Video generation request on topic: '{}' for patient: '{}'", topic, patientName);
        
        VideoGenerateRequest videoReq = VideoGenerateRequest.builder()
                .topic(topic)
                .patientId(userId)
                .patientName(patientName)
                .memoryType("PHOTO")
                .duration(60)
                .build();
                
        try {
            VideoGenerateResponse video = videoScriptService.generateVideoScript(videoReq);
            String nameString = (patientName != null && !patientName.trim().isEmpty()) ? " pour " + patientName : "";
            
            return VoiceCommandResponse.builder()
                    .type("ACTION")
                    .message(String.format("🎬 Vidéo personnalisée sur '%s'%s générée avec succès !",
                            topic, nameString))
                    .data(video)
                    .build();
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Impossible de générer la vidéo: " + e.getMessage())
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

        String aiResponse;
        try {
            ChatClient chatClient = chatClientBuilder.build();
            aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("OpenAI API call failed for voice command: {}", e.getMessage());
            aiResponse = String.format("""
                    🤖 I didn't understand the command "%s".
                    
                    Here are the available commands:
                    • **borrow [name]** — Borrow medical equipment
                    • **return [name]** — Return equipment
                    • **quiz about [topic]** — Generate a memory quiz
                    • **status** — View your scores and active loans
                    
                    Try one of these commands! 😊""", command);
        }

        return VoiceCommandResponse.builder()
                .type("INFO")
                .message(aiResponse)
                .build();
    }
}
