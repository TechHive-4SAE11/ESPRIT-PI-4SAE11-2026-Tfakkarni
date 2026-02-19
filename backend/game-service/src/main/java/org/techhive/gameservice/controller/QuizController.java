package org.techhive.gameservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techhive.gameservice.dto.QuizDTO;
import org.techhive.gameservice.entity.Quiz;
import org.techhive.gameservice.service.IQuizService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/games/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final IQuizService quizService;

    @PostMapping
    public ResponseEntity<QuizDTO> createQuiz(@Valid @RequestBody QuizDTO quizDTO) {
        log.info("Creating new quiz with topic: {}", quizDTO.getTopic());

        Quiz createdQuiz = quizService.createQuiz(quizDTO);
        if (createdQuiz == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(QuizDTO.fromEntity(createdQuiz), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuizDTO> updateQuiz(@PathVariable Long id, @Valid @RequestBody QuizDTO quizDTO) {
        log.info("Updating quiz with ID: {}", id);

        quizDTO.setId(id);
        Quiz updatedQuiz = quizService.updateQuiz(quizDTO);
        if (updatedQuiz == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(QuizDTO.fromEntity(updatedQuiz));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        log.info("Deleting quiz with ID: {}", id);

        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizDTO> getQuizById(@PathVariable Long id) {
        log.info("Fetching quiz with ID: {}", id);

        Quiz quiz = quizService.getQuizById(id);
        if (quiz == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(QuizDTO.fromEntity(quiz));
    }

    @GetMapping
    public ResponseEntity<List<QuizDTO>> getAllQuizzes() {
        log.info("Fetching all quizzes");

        List<Quiz> quizzes = quizService.getAllQuizzes();
        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(QuizDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<QuizDTO>> getQuizzesByCaregiverId(@PathVariable Long caregiverId) {
        log.info("Fetching quizzes for caregiver ID: {}", caregiverId);

        List<Quiz> quizzes = quizService.getQuizzesByCaregiverId(caregiverId);
        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(QuizDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/search")
    public ResponseEntity<List<QuizDTO>> searchQuizzesByTopic(@RequestParam String topic) {
        log.info("Searching quizzes with topic: {}", topic);

        List<Quiz> quizzes = quizService.searchQuizzesByTopic(topic);
        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(QuizDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/caregiver/{caregiverId}/recent")
    public ResponseEntity<List<QuizDTO>> getRecentQuizzesByCaregiver(
            @PathVariable Long caregiverId, @RequestParam(defaultValue = "5") int limit) {
        log.info("Fetching recent {} quizzes for caregiver ID: {}", limit, caregiverId);

        List<Quiz> quizzes = quizService.getRecentQuizzesByCaregiver(caregiverId, limit);
        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(QuizDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<QuizDTO>> getQuizzesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Fetching quizzes between {} and {}", startDate, endDate);

        List<Quiz> quizzes = quizService.getQuizzesByDateRange(startDate, endDate);
        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(QuizDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/min-score/{minScore}")
    public ResponseEntity<List<QuizDTO>> getQuizzesWithMinScore(@PathVariable Integer minScore) {
        log.info("Fetching quizzes with minimum score: {}", minScore);

        List<Quiz> quizzes = quizService.getQuizzesWithMinScore(minScore);
        List<QuizDTO> quizDTOs = quizzes.stream()
                .map(QuizDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(quizDTOs);
    }

    @GetMapping("/caregiver/{caregiverId}/count")
    public ResponseEntity<Long> getQuizCountByCaregiver(@PathVariable Long caregiverId) {
        log.info("Getting quiz count for caregiver ID: {}", caregiverId);

        long count = quizService.getQuizCountByCaregiver(caregiverId);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<QuizDTO> startQuiz(@PathVariable Long id) {
        log.info("Starting quiz with ID: {}", id);

        Quiz quiz = quizService.startQuiz(id);
        if (quiz == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(QuizDTO.fromEntity(quiz));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<QuizDTO> completeQuiz(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer score = request.get("score");
        log.info("Completing quiz with ID: {} with score: {}", id, score);

        Quiz quiz = quizService.completeQuiz(id, score);
        if (quiz == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(QuizDTO.fromEntity(quiz));
    }

    @GetMapping("/caregiver/{caregiverId}/average-score")
    public ResponseEntity<Double> getAverageScoreByCaregiver(@PathVariable Long caregiverId) {
        log.info("Getting average score for caregiver ID: {}", caregiverId);

        double averageScore = quizService.getAverageScoreByCaregiver(caregiverId);
        return ResponseEntity.ok(averageScore);
    }

    @GetMapping("/caregiver/{caregiverId}/weak-topics")
    public ResponseEntity<List<String>> getWeakTopicsByCaregiver(@PathVariable Long caregiverId) {
        log.info("Getting weak topics for caregiver ID: {}", caregiverId);

        List<String> weakTopics = quizService.getWeakTopicsByCaregiver(caregiverId);
        return ResponseEntity.ok(weakTopics);
    }
}
