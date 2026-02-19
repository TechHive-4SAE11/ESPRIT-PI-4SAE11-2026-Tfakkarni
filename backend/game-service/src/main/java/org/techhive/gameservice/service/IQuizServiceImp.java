package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.gameservice.dto.QuizDTO;
import org.techhive.gameservice.entity.Question;
import org.techhive.gameservice.entity.Quiz;
import org.techhive.gameservice.repository.QuizRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IQuizServiceImp implements IQuizService {

    private final QuizRepository quizRepository;
    private final IQuestionService questionService;

    @Override
    public Quiz createQuiz(QuizDTO quizDTO) {
        Quiz quiz = quizDTO.toEntity();

        if (!validateQuiz(quiz)) {
            log.error("Invalid quiz data");
            return null;
        }

        return quizRepository.save(quiz);
    }

    @Override
    public Quiz getQuizById(long id) {
        return quizRepository.findById(id).orElse(null);
    }

    @Override
    public Quiz updateQuiz(QuizDTO quizDTO) {
        if (!quizRepository.existsById(quizDTO.getId())) {
            log.error("Quiz not found with id: {}", quizDTO.getId());
            return null;
        }

        Quiz quiz = quizDTO.toEntity();

        if (!validateQuiz(quiz)) {
            log.error("Invalid quiz data");
            return null;
        }

        return quizRepository.save(quiz);
    }

    @Override
    public void deleteQuiz(long id) {
        if (!quizRepository.existsById(id)) {
            log.error("Quiz not found with id: {}", id);
            return;
        }
        quizRepository.deleteById(id);
    }

    @Override
    public List<Quiz> getAllQuizzes() {
        return StreamSupport.stream(quizRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public List<Quiz> getQuizzesByCaregiverId(Long caregiverId) {
        return quizRepository.findByCaregiverId(caregiverId);
    }

    @Override
    public List<Quiz> searchQuizzesByTopic(String topic) {
        if (topic == null || topic.trim().isEmpty()) {
            return List.of();
        }
        return quizRepository.findByTopicContainingIgnoreCase(topic);
    }

    @Override
    public List<Quiz> getRecentQuizzesByCaregiver(Long caregiverId, int limit) {
        return quizRepository.findByCaregiverIdOrderByDateTakenDesc(caregiverId)
                .stream()
                .limit(limit)
                .toList();
    }

    @Override
    public List<Quiz> getQuizzesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return quizRepository.findByDateTakenBetween(startDate, endDate);
    }

    @Override
    public List<Quiz> getQuizzesWithMinScore(Integer minScore) {
        return quizRepository.findQuizzesWithMinScore(minScore);
    }

    @Override
    public long getQuizCountByCaregiver(Long caregiverId) {
        return quizRepository.countByCaregiverId(caregiverId);
    }

    @Override
    public Quiz startQuiz(Long quizId) {
        Quiz quiz = getQuizById(quizId);
        if (quiz == null) {
            log.error("Quiz not found with id: {}", quizId);
            return null;
        }

        quiz.setDateTaken(LocalDateTime.now());
        quiz.setTotalScore(0);

        return quizRepository.save(quiz);
    }

    @Override
    public Quiz completeQuiz(Long quizId, Integer score) {
        Quiz quiz = getQuizById(quizId);
        if (quiz == null) {
            log.error("Quiz not found with id: {}", quizId);
            return null;
        }

        quiz.setTotalScore(score);

        return quizRepository.save(quiz);
    }

    @Override
    public double getAverageScoreByCaregiver(Long caregiverId) {
        List<Quiz> quizzes = quizRepository.findByCaregiverId(caregiverId);

        if (quizzes.isEmpty()) {
            return 0.0;
        }

        double totalScore = quizzes.stream()
                .filter(q -> q.getTotalScore() != null)
                .mapToInt(Quiz::getTotalScore)
                .sum();

        long completedQuizzes = quizzes.stream()
                .filter(q -> q.getTotalScore() != null)
                .count();

        return completedQuizzes > 0 ? totalScore / completedQuizzes : 0.0;
    }

    @Override
    public List<String> getWeakTopicsByCaregiver(Long caregiverId) {
        List<Quiz> quizzes = quizRepository.findByCaregiverId(caregiverId);

        return quizzes.stream()
                .filter(q -> q.getQuestions() != null)
                .flatMap(q -> q.getQuestions().stream())
                .filter(q -> {
                    // Logique pour déterminer si une question a été mal répondue
                    // Ceci est un exemple - à adapter selon ta logique métier
                    return false;
                })
                .map(Question::getText)
                .distinct()
                .toList();
    }

    private boolean validateQuiz(Quiz quiz) {
        if (quiz.getTopic() == null || quiz.getTopic().trim().isEmpty()) {
            log.error("Quiz topic cannot be empty");
            return false;
        }
        if (quiz.getCaregiverId() == null) {
            log.error("Caregiver ID is required");
            return false;
        }
        return true;
    }
}