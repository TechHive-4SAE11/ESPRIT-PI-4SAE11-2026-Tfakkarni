package org.techhive.assistantservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.techhive.assistantservice.client.dto.AnswerDTO;
import org.techhive.assistantservice.client.dto.QuestionDTO;
import org.techhive.assistantservice.client.dto.QuizDTO;

import java.util.List;

@FeignClient(name = "game-service", url = "${feign.game-service.url:http://localhost:18082}")
public interface GameServiceClient {

    // ── Quiz endpoints ──
    @PostMapping("/api/games/quiz")
    QuizDTO createQuiz(@RequestBody QuizDTO quizDTO);

    @GetMapping("/api/games/quiz/{id}")
    QuizDTO getQuizById(@PathVariable("id") Long id);

    @GetMapping("/api/games/quiz")
    List<QuizDTO> getAllQuizzes();

    @GetMapping("/api/games/quiz/caregiver/{caregiverId}")
    List<QuizDTO> getQuizzesByCaregiverId(@PathVariable("caregiverId") Long caregiverId);

    @GetMapping("/api/games/quiz/caregiver/{caregiverId}/average-score")
    Double getAverageScoreByCaregiver(@PathVariable("caregiverId") Long caregiverId);

    @GetMapping("/api/games/quiz/caregiver/{caregiverId}/weak-topics")
    List<String> getWeakTopicsByCaregiver(@PathVariable("caregiverId") Long caregiverId);

    @GetMapping("/api/games/quiz/caregiver/{caregiverId}/count")
    Long getQuizCountByCaregiver(@PathVariable("caregiverId") Long caregiverId);

    // ── Question endpoints ──
    @PostMapping("/api/games/quiz/questions")
    QuestionDTO createQuestion(@RequestBody QuestionDTO questionDTO);

    @GetMapping("/api/games/quiz/questions/quiz/{quizId}")
    List<QuestionDTO> getQuestionsByQuizId(@PathVariable("quizId") Long quizId);

    // ── Answer endpoints ──
    @PostMapping("/api/games/quiz/answer")
    AnswerDTO createAnswer(@RequestBody AnswerDTO answerDTO);

    @PostMapping("/api/games/quiz/answer/batch")
    List<AnswerDTO> createAnswersBatch(@RequestBody List<AnswerDTO> answerDTOs);
}
