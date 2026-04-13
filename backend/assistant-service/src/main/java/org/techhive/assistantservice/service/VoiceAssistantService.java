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
                    .message("Sorry, an error occurred: " + e.getMessage())
                    .sessionId(request.getSessionId())
                    .build();
        }
    }

    private String classifyIntentWithAI(String command) {
        String prompt = String.format("""
                You are an intent classifier for a bilingual (English/French) medical application voice assistant.
                Classify the following user command into exactly one of these actions:
                - BORROW: Borrow or request medical equipment (wheelchair, bed, etc.) — FR: emprunter, demander
                - RETURN: Return or give back equipment — FR: rendre, retourner
                - QUIZ: Generate, create or start a quiz on a given topic — FR: quiz, générer quiz
                - STATUS: Ask for status, scores, or active loans — FR: statut, état
                - VIDEO: Create or generate a memory or therapeutic video — FR: vidéo, créer vidéo
                - UNKNOWN: If it does not match anything above.
                
                The user may speak in English OR French. Understand both languages.
                
                Command: "%s"
                
                Respond ONLY with a JSON object containing "action" and "parameter" (the subject/item, empty if none).
                Example:
                {"action": "BORROW", "parameter": "wheelchair"}
                {"action": "QUIZ", "parameter": "geography"}
                {"action": "VIDEO", "parameter": "childhood memories"}
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
                    .message("Unable to contact the medical service. Please try again later.")
                    .build();
        }

        if (results.isEmpty()) {
            return VoiceCommandResponse.builder()
                    .type("INFO")
                    .message("No equipment found with the name '" + equipmentName + "'.")
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
                    .message("The equipment '" + equipmentName + "' is not currently available.")
                    .build();
        }

        // Create loan
        EquipmentLoanDTO loanDTO = EquipmentLoanDTO.builder()
                .equipmentId(available.getId())
                .borrowerId(userId)
                .loanDate(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(14))
                .purpose("Borrowed via voice assistant")
                .status("ACTIVE")
                .build();

        try {
            EquipmentLoanDTO createdLoan = medicalServiceClient.borrowEquipment(loanDTO);

            return VoiceCommandResponse.builder()
                    .type("ACTION")
                    .message(String.format("✅ Equipment '%s' borrowed successfully! Return expected within 14 days.",
                            available.getName()))
                    .data(createdLoan)
                    .build();
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Borrow failed: " + e.getMessage())
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
                    .message("Unable to retrieve your active loans.")
                    .build();
        }

        if (activeLoans.isEmpty()) {
            return VoiceCommandResponse.builder()
                    .type("INFO")
                    .message("You have no active loans to return.")
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
                    .message("No loan matching '" + equipmentName + "'.\nYour active loans:" + loansList)
                    .build();
        }

        try {
            EquipmentLoanDTO returnedLoan = medicalServiceClient.returnEquipment(matchingLoan.getId());
            return VoiceCommandResponse.builder()
                    .type("ACTION")
                    .message(String.format("✅ Equipment '%s' returned successfully!",
                            matchingLoan.getEquipmentName()))
                    .data(returnedLoan)
                    .build();
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Return failed: " + e.getMessage())
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
                    .message(String.format("🎯 Quiz '%s' created with %d questions! Ready to start?",
                            topic, generatedQuiz.getQuestions() != null ? generatedQuiz.getQuestions().size() : 0))
                    .data(generatedQuiz)
                    .build();
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Unable to generate the quiz: " + e.getMessage())
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
            String nameString = (patientName != null && !patientName.trim().isEmpty()) ? " for " + patientName : "";
            
            return VoiceCommandResponse.builder()
                    .type("ACTION")
                    .message(String.format("🎬 Personalized video about '%s'%s generated successfully!",
                            topic, nameString))
                    .data(video)
                    .build();
        } catch (Exception e) {
            return VoiceCommandResponse.builder()
                    .type("ERROR")
                    .message("Unable to generate the video: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Handle "statut" command - summarize quiz scores + active loans.
     */
    private VoiceCommandResponse handleStatusCommand(Long userId) {
        log.info("Status request for user: {}", userId);

        StringBuilder status = new StringBuilder("📊 **Your Status:**\n\n");

        // Quiz stats
        try {
            Long quizCount = gameServiceClient.getQuizCountByCaregiver(userId);
            Double avgScore = gameServiceClient.getAverageScoreByCaregiver(userId);
            status.append(String.format("🧠 **Quiz:** %d quizzes completed, average score: %.1f%%\n",
                    quizCount != null ? quizCount : 0,
                    avgScore != null ? avgScore : 0.0));

            List<String> weakTopics = gameServiceClient.getWeakTopicsByCaregiver(userId);
            if (weakTopics != null && !weakTopics.isEmpty()) {
                status.append("⚠️ Topics to improve: ").append(String.join(", ", weakTopics)).append("\n");
            }
        } catch (Exception e) {
            status.append("🧠 Quiz: data not available\n");
        }

        status.append("\n");

        // Loan stats
        try {
            List<EquipmentLoanDTO> activeLoans = medicalServiceClient.getActiveLoansByBorrower(userId);
            status.append(String.format("🏥 **Active Loans:** %d\n", activeLoans.size()));
            for (EquipmentLoanDTO loan : activeLoans) {
                status.append(String.format("  - %s (due: %s)\n",
                        loan.getEquipmentName() != null ? loan.getEquipmentName() : "Equipment #" + loan.getEquipmentId(),
                        loan.getDueDate() != null ? loan.getDueDate() : "N/A"));
            }
        } catch (Exception e) {
            status.append("🏥 Loans: data not available\n");
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
                1. "borrow [name]" / "emprunter [nom]" - Borrow medical equipment
                2. "return [name]" / "rendre [nom]" - Return medical equipment
                3. "quiz about [topic]" / "quiz sur [sujet]" - Generate a memory quiz
                4. "status" / "statut" - Show quiz scores and active equipment loans
                5. "video about [topic]" / "vidéo sur [sujet]" - Generate a memory video
                
                If the command matches one of these actions, suggest the correct command format.
                Otherwise, provide a helpful response.
                
                IMPORTANT: Respond in the SAME language the user used (English or French).
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
