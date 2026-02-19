package org.techhive.gameservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.QuestionDTO;
import org.techhive.gameservice.entity.Question;
import org.techhive.gameservice.service.IQuestionService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/games/quiz/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final IQuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionDTO> createQuestion(@Valid @RequestBody QuestionDTO questionDTO) {
        log.info("Creating new question for quiz ID: {}", questionDTO.getQuizId());

        Question createdQuestion = questionService.createQuestion(questionDTO);
        if (createdQuestion == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(QuestionDTO.fromEntity(createdQuestion), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionDTO> updateQuestion(@PathVariable Long id, @Valid @RequestBody QuestionDTO questionDTO) {
        log.info("Updating question with ID: {}", id);

        questionDTO.setId(id);
        Question updatedQuestion = questionService.updateQuestion(questionDTO);
        if (updatedQuestion == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(QuestionDTO.fromEntity(updatedQuestion));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        log.info("Deleting question with ID: {}", id);

        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getQuestionById(@PathVariable Long id) {
        log.info("Fetching question with ID: {}", id);

        Question question = questionService.getQuestionById(id);
        if (question == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(QuestionDTO.fromEntity(question));
    }

    @GetMapping
    public ResponseEntity<List<QuestionDTO>> getAllQuestions() {
        log.info("Fetching all questions");

        List<Question> questions = questionService.getAllQuestions();
        List<QuestionDTO> questionDTOs = questions.stream()
                .map(QuestionDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(questionDTOs);
    }

    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByQuizId(@PathVariable Long quizId) {
        log.info("Fetching questions for quiz ID: {}", quizId);

        List<Question> questions = questionService.getQuestionsByQuizId(quizId);
        List<QuestionDTO> questionDTOs = questions.stream()
                .map(QuestionDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(questionDTOs);
    }

    @GetMapping("/difficulty/{level}")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByDifficultyLevel(@PathVariable Integer level) {
        log.info("Fetching questions with difficulty level: {}", level);

        List<Question> questions = questionService.getQuestionsByDifficultyLevel(level);
        List<QuestionDTO> questionDTOs = questions.stream()
                .map(QuestionDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(questionDTOs);
    }

    @DeleteMapping("/quiz/{quizId}")
    public ResponseEntity<Void> deleteQuestionsByQuizId(@PathVariable Long quizId) {
        log.info("Deleting all questions for quiz ID: {}", quizId);

        questionService.deleteQuestionsByQuizId(quizId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/quiz/{quizId}/count")
    public ResponseEntity<Long> getQuestionCountByQuizId(@PathVariable Long quizId) {
        log.info("Getting question count for quiz ID: {}", quizId);

        long count = questionService.getQuestionCountByQuizId(quizId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/search")
    public ResponseEntity<List<QuestionDTO>> searchQuestions(@RequestParam String keyword) {
        log.info("Searching questions with keyword: {}", keyword);

        List<Question> questions = questionService.searchQuestions(keyword);
        List<QuestionDTO> questionDTOs = questions.stream()
                .map(QuestionDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(questionDTOs);
    }

    @GetMapping("/quiz/{quizId}/difficulty/{level}")
    public ResponseEntity<List<QuestionDTO>> getQuestionsByQuizAndDifficulty(
            @PathVariable Long quizId, @PathVariable Integer level) {
        log.info("Fetching questions for quiz ID: {} with difficulty: {}", quizId, level);

        List<Question> questions = questionService.getQuestionsByQuizAndDifficulty(quizId, level);
        List<QuestionDTO> questionDTOs = questions.stream()
                .map(QuestionDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(questionDTOs);
    }

    @GetMapping("/quiz/{quizId}/total-points")
    public ResponseEntity<Integer> calculateTotalPoints(@PathVariable Long quizId) {
        log.info("Calculating total points for quiz ID: {}", quizId);

        int totalPoints = questionService.calculateTotalPoints(quizId);
        return ResponseEntity.ok(totalPoints);
    }
}
