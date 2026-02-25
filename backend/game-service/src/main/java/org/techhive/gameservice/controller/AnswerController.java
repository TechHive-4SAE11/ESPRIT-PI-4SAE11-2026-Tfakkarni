package org.techhive.gameservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.*;
import org.techhive.gameservice.entity.Answer;
import org.techhive.gameservice.service.IAnswerService;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RestController
@RequestMapping("/api/games/quiz/answer")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnswerController {

    private final IAnswerService answerService;

    @PostMapping
    @Transactional
    public ResponseEntity<AnswerDTO> createAnswer(@Valid @RequestBody AnswerDTO answerDTO) {

        Answer createdAnswer = answerService.createAnswer(answerDTO);
        return new ResponseEntity<>(AnswerDTO.fromEntity(createdAnswer), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<AnswerDTO> updateAnswer(@PathVariable Long id, @Valid @RequestBody AnswerDTO answerDTO) {
        answerDTO.setId(id);
        Answer updatedAnswer = answerService.updateAnswer(answerDTO);
        return ResponseEntity.ok(AnswerDTO.fromEntity(updatedAnswer));
    }

    @DeleteMapping("deleteAnswer/{id}")
    @Transactional
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long id) {
        answerService.deleteAnswer(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getAnswerById/{id}")
    public ResponseEntity<AnswerDTO> getAnswerById(@PathVariable Long id) {
        Answer answer = answerService.getAnswerById(id);
        if (answer == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(AnswerDTO.fromEntity(answer));
    }

    @GetMapping
    public ResponseEntity<List<AnswerDTO>> getAllAnswers() {
        List<Answer> answers = answerService.getAllAnswers();
        List<AnswerDTO> answerDTOs = answers.stream()
                .map(AnswerDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(answerDTOs);
    }

    /**
     * Get answers by question ID
     */
    @GetMapping("/question/{questionId}")
    public ResponseEntity<List<AnswerDTO>> getAnswersByQuestionId(@PathVariable Long questionId) {

        List<Answer> answers = answerService.getAnswersByQuestionId(questionId);
        List<AnswerDTO> answerDTOs = answers.stream()
                .map(AnswerDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(answerDTOs);
    }

    /**
     * Get correct answer for a question
     */
    @GetMapping("/question/{questionId}/correct")
    public ResponseEntity<AnswerDTO> getCorrectAnswerByQuestionId(@PathVariable Long questionId) {

        Answer correctAnswer = answerService.getCorrectAnswerByQuestionId(questionId);
        if (correctAnswer == null) {
            return ResponseEntity.notFound().build(); // Retourne 404
        }
        return ResponseEntity.ok(AnswerDTO.fromEntity(correctAnswer));
    }

    /**
     * Validate an answer
     */
    @PostMapping("/validate")
    @Transactional
    public ResponseEntity<ValidationResponseDTO> validateAnswer(@RequestBody ValidationRequestDTO request) {
        log.info("Validating answer ID: {} for question ID: {}", request.getAnswerId(), request.getQuestionId());

        boolean isValid = answerService.validateAnswer(request.getQuestionId(), request.getAnswerId());
        Answer answer = answerService.getAnswerById(request.getAnswerId());

        ValidationResponseDTO response = ValidationResponseDTO.builder()
                .valid(isValid)
                .answerId(request.getAnswerId())
                .questionId(request.getQuestionId())
                .explanation(isValid ? "Correct answer!" : answer.getExplanation())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Submit an answer (for quiz taking)
     */
    @PostMapping("/submit")
    @Transactional
    public ResponseEntity<SubmissionResponseDTO> submitAnswer(@RequestBody SubmissionRequestDTO request) {
        log.info("Submitting answer ID: {} for question ID: {} in quiz ID: {}",
                request.getAnswerId(), request.getQuestionId(), request.getQuizId());

        Answer answer = answerService.getAnswerById(request.getAnswerId());
        boolean isCorrect = answer.isCorrect();

        // Save the user's selection
        answerService.recordAnswerSelection(request.getQuizId(), request.getQuestionId(), request.getAnswerId());

        SubmissionResponseDTO response = SubmissionResponseDTO.builder()
                .correct(isCorrect)
                .answerId(request.getAnswerId())
                .questionId(request.getQuestionId())
                .quizId(request.getQuizId())
                .explanation(answer.getExplanation())
                .feedback(getFeedbackMessage(isCorrect, answer.getExplanation()))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Batch create answers
     */
    @PostMapping("/batch")
    @Transactional
    public ResponseEntity<List<AnswerDTO>> createAnswersBatch(@Valid @RequestBody List<AnswerDTO> answerDTOs) {
        log.info("=== BATCH ANSWERS REQUEST ===");
        log.info("Number of answers: {}", answerDTOs.size());

        for (int i = 0; i < answerDTOs.size(); i++) {
            AnswerDTO dto = answerDTOs.get(i);
            log.info("Answer {}: questionId={}, text='{}', isCorrect={}, explanation='{}'",
                    i, dto.getQuestionId(), dto.getText(), dto.getIsCorrect(), dto.getExplanation());
        }

        try {
            List<Answer> createdAnswers = answerService.createAnswersBatch(answerDTOs);
            List<AnswerDTO> createdDTOs = createdAnswers.stream()
                    .map(AnswerDTO::fromEntity)
                    .toList();
            return new ResponseEntity<>(createdDTOs, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating answers batch", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if answer is correct
     */
    @GetMapping("/{id}/is-correct")
    public ResponseEntity<BooleanResponseDTO> isAnswerCorrect(@PathVariable Long id) {
        log.info("Checking if answer ID: {} is correct", id);

        Answer answer = answerService.getAnswerById(id);
        BooleanResponseDTO response = BooleanResponseDTO.builder()
                .value(answer.isCorrect())
                .message(answer.isCorrect() ? "Answer is correct" : "Answer is incorrect")
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Get answer count for a question
     */
    @GetMapping("/question/{questionId}/count")
    public ResponseEntity<CountResponseDTO> getAnswerCountByQuestionId(@PathVariable Long questionId) {
        log.info("Getting answer count for question ID: {}", questionId);

        long count = answerService.getAnswerCountByQuestionId(questionId);
        CountResponseDTO response = CountResponseDTO.builder()
                .count(count)
                .questionId(questionId)
                .build();
        return ResponseEntity.ok(response);
    }

    /**
     * Search answers by text
     */
    @GetMapping("/search")
    public ResponseEntity<List<AnswerDTO>> searchAnswers(@RequestParam String keyword) {
        log.info("Searching answers with keyword: {}", keyword);

        List<Answer> answers = answerService.searchAnswers(keyword);
        List<AnswerDTO> answerDTOs = answers.stream()
                .map(AnswerDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(answerDTOs);
    }

    private String getFeedbackMessage(boolean isCorrect, String explanation) {
        if (isCorrect) {
            return "✅ Correct! " + (explanation != null ? explanation : "Well done!");
        } else {
            return "❌ Incorrect. " + (explanation != null ? explanation : "Try again!");
        }
    }
}
