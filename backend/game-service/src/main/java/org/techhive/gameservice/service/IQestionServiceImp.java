package org.techhive.gameservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.techhive.gameservice.dto.QuestionDTO;
import org.techhive.gameservice.entity.Question;
import org.techhive.gameservice.entity.Quiz;
import org.techhive.gameservice.repository.QuestionRepository;
import org.techhive.gameservice.repository.QuizRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IQestionServiceImp implements IQuestionService{

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;

    @Override
    public Question createQuestion(QuestionDTO questionDTO) {
        Question question = questionDTO.toEntity();

        Quiz quiz = quizRepository.findById(questionDTO.getQuizId())
                .orElse(null);
        if (quiz == null) {
            log.error("Quiz not found with id: {}", questionDTO.getQuizId());
            return null;
        }
        question.setQuiz(quiz);
        if (!validateQuestion(question)) {
            log.error("Invalid question data");
            return null;
        }
        question.setQuiz(quiz);
        return questionRepository.save(question);
    }

    @Override
    public Question getQuestionById(long id) {
        return questionRepository.findById(id).orElse(null);
    }

    @Override
    public Question updateQuestion(QuestionDTO questionDTO) {
        if (!questionRepository.existsById(questionDTO.getId())) {
            log.error("Question not found with id: {}", questionDTO.getId());
            return null;
        }

        Question question = questionDTO.toEntity();

        // Set the quiz
        Quiz quiz = quizRepository.findById(questionDTO.getQuizId())
                .orElse(null);

        if (quiz == null) {
            log.error("Quiz not found with id: {}", questionDTO.getQuizId());
            return null;
        }

        question.setQuiz(quiz);

        if (!validateQuestion(question)) {
            log.error("Invalid question data");
            return null;
        }

        return questionRepository.save(question);
    }

    @Override
    public void deleteQuestion(long id) {
        if (!questionRepository.existsById(id)) {
            log.error("Question not found with id: {}", id);
            return;
        }
        questionRepository.deleteById(id);
    }

    @Override
    public List<Question> getAllQuestions() {
        return StreamSupport.stream(questionRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public List<Question> getQuestionsByQuizId(Long quizId) {
        return questionRepository.findByQuizId(quizId);
    }

    @Override
    public List<Question> getQuestionsByDifficultyLevel(Integer difficultyLevel) {
        return questionRepository.findByDifficultyLevel(difficultyLevel);
    }

    @Override
    @Transactional
    public void deleteQuestionsByQuizId(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            log.error("Quiz not found with id: {}", quizId);
            return;
        }
        questionRepository.deleteByQuizId(quizId);
    }

    @Override
    public long getQuestionCountByQuizId(Long quizId) {
        return questionRepository.countByQuizId(quizId);
    }

    @Override
    public List<Question> searchQuestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return questionRepository.findByTextContainingIgnoreCase(keyword);
    }

    @Override
    public List<Question> getQuestionsByQuizAndDifficulty(Long quizId, Integer difficultyLevel) {
        return questionRepository.findByQuizIdAndDifficultyLevel(quizId, difficultyLevel);
    }

    @Override
    public boolean validateQuestion(QuestionDTO questionDTO) {
        if (questionDTO.getText() == null || questionDTO.getText().trim().isEmpty()) {
            log.error("Question text cannot be empty");
            return false;
        }
        if (questionDTO.getDifficultyLevel() == null || questionDTO.getDifficultyLevel() < 1) {
            log.error("Difficulty level must be at least 1");
            return false;
        }
        return true;
    }

    @Override
    public int calculateTotalPoints(Long quizId) {
        List<Question> questions = questionRepository.findByQuizId(quizId);
        return questions.stream()
                .mapToInt(q -> q.getDifficultyLevel() * 10) // Exemple: points = difficulté * 10
                .sum();
    }

    private boolean validateQuestion(Question question) {
        if (question.getText() == null || question.getText().trim().isEmpty()) {
            log.error("Question text cannot be empty");
            return false;
        }
        if (question.getDifficultyLevel() == null || question.getDifficultyLevel() < 1) {
            log.error("Difficulty level must be at least 1");
            return false;
        }
        if (question.getQuiz() == null) {
            log.error("Question must be associated with a quiz");
            return false;
        }
        return true;
    }


}
