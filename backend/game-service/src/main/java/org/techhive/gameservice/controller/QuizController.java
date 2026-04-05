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
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RestController
@RequestMapping("/api/games/quiz")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizController {

    private final IQuizService quizService;

    @PostMapping
    @Transactional
    public ResponseEntity<QuizDTO> createQuiz(@Valid @RequestBody QuizDTO quizDTO) {
        log.info("Creating new quiz with topic: {}", quizDTO.getTopic());

        Quiz createdQuiz = quizService.createQuiz(quizDTO);
        if (createdQuiz == null) {
            return ResponseEntity.badRequest().build();
        }
        return new ResponseEntity<>(QuizDTO.fromEntity(createdQuiz), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Transactional
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
    @Transactional
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
    public ResponseEntity<?> getAllQuizzes() {
        log.info("Fetching all quizzes");

        try {
            List<Quiz> quizzes = quizService.getAllQuizzes();
            List<QuizDTO> quizDTOs = quizzes.stream()
                    .map(QuizDTO::fromEntity)
                    .toList();
            return ResponseEntity.ok(quizDTOs);
        } catch (Exception e) {
            log.error("Error fetching all quizzes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch quizzes: " + e.getMessage()));
        }
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<?> getQuizzesByCaregiverId(@PathVariable Long caregiverId) {
        log.info("Fetching quizzes for caregiver ID: {}", caregiverId);

        try {
            List<Quiz> quizzes = quizService.getQuizzesByCaregiverId(caregiverId);
            List<QuizDTO> quizDTOs = quizzes.stream()
                    .map(QuizDTO::fromEntity)
                    .toList();
            return ResponseEntity.ok(quizDTOs);
        } catch (Exception e) {
            log.error("Error fetching quizzes for caregiver {}: {}", caregiverId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch quizzes: " + e.getMessage()));
        }
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
    public ResponseEntity<?> getRecentQuizzesByCaregiver(
            @PathVariable Long caregiverId, @RequestParam(defaultValue = "5") int limit) {
        log.info("Fetching recent {} quizzes for caregiver ID: {}", limit, caregiverId);

        try {
            List<Quiz> quizzes = quizService.getRecentQuizzesByCaregiver(caregiverId, limit);
            List<QuizDTO> quizDTOs = quizzes.stream()
                    .map(QuizDTO::fromEntity)
                    .toList();
            return ResponseEntity.ok(quizDTOs);
        } catch (Exception e) {
            log.error("Error fetching recent quizzes for caregiver {}: {}", caregiverId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch recent quizzes: " + e.getMessage()));
        }
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
    public ResponseEntity<?> getQuizCountByCaregiver(@PathVariable Long caregiverId) {
        log.info("Getting quiz count for caregiver ID: {}", caregiverId);

        try {
            long count = quizService.getQuizCountByCaregiver(caregiverId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting quiz count for caregiver {}: {}", caregiverId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get quiz count: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    @Transactional
    public ResponseEntity<QuizDTO> startQuiz(@PathVariable Long id) {
        log.info("Starting quiz with ID: {}", id);

        Quiz quiz = quizService.startQuiz(id);
        if (quiz == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(QuizDTO.fromEntity(quiz));
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public ResponseEntity<QuizDTO> completeQuiz(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        Integer score = request.get("score");
        Integer levelReached = request.get("levelReached");
        log.info("Completing quiz with ID: {} with score: {} and levelReached: {}", id, score, levelReached);

        Quiz quiz = quizService.completeQuiz(id, score, levelReached);
        if (quiz == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(QuizDTO.fromEntity(quiz));
    }

    @GetMapping("/caregiver/{caregiverId}/average-score")
    public ResponseEntity<?> getAverageScoreByCaregiver(@PathVariable Long caregiverId) {
        log.info("Getting average score for caregiver ID: {}", caregiverId);

        try {
            double averageScore = quizService.getAverageScoreByCaregiver(caregiverId);
            return ResponseEntity.ok(averageScore);
        } catch (Exception e) {
            log.error("Error getting average score for caregiver {}: {}", caregiverId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get average score: " + e.getMessage()));
        }
    }

    @GetMapping("/caregiver/{caregiverId}/weak-topics")
    public ResponseEntity<?> getWeakTopicsByCaregiver(@PathVariable Long caregiverId) {
        log.info("Getting weak topics for caregiver ID: {}", caregiverId);

        try {
            List<String> weakTopics = quizService.getWeakTopicsByCaregiver(caregiverId);
            return ResponseEntity.ok(weakTopics);
        } catch (Exception e) {
            log.error("Error getting weak topics for caregiver {}: {}", caregiverId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get weak topics: " + e.getMessage()));
        }
    }
}
