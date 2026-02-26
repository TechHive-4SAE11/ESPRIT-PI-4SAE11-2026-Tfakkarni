package org.techhive.gameservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.techhive.gameservice.dto.AnswerDTO;
import org.techhive.gameservice.entity.Answer;
import org.techhive.gameservice.entity.Question;
import org.techhive.gameservice.repository.AnswerRepository;
import org.techhive.gameservice.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class IAnswerServiceImp implements IAnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    @Override
    public Answer createAnswer(AnswerDTO answerDTO) {
        Answer answer = answerDTO.toEntity();
        Question question = questionRepository.findById(answerDTO.getQuestionId())
                .orElse(null);
        answer.setQuestion(question);

        validateAnswer(answer);
        return answerRepository.save(answer);
    }

    @Override
    public Answer getAnswerById(long id) {
        return answerRepository.findById(id).orElse(null);
    }

    @Override
    public Answer updateAnswer(AnswerDTO answerDTO) {
        if (!answerRepository.existsById(answerDTO.getId())) {
            log.error("Answer not found with id: {}", answerDTO.getId());
            return null;
        }

        Answer answer = answerDTO.toEntity();
        Question question = questionRepository.findById(answerDTO.getQuestionId())
                .orElse(null);
        answer.setQuestion(question);

        validateAnswer(answer);
        return answerRepository.save(answer);
    }

    @Override
    public void deleteAnswer(long id) {
        if (!answerRepository.existsById(id)) {
            log.error("Answer not found with id: {}", id);
            return;
        }
        answerRepository.deleteById(id);
    }

    @Override
    public List<Answer> getAllAnswers() {
        return StreamSupport.stream(answerRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public List<Answer> getAnswersByQuestionId(Long questionId) {
        return answerRepository.findByQuestionId(questionId);
    }

    @Override
    public Answer getCorrectAnswerByQuestionId(Long questionId) {
        return answerRepository.findByQuestionIdAndIsCorrectTrue(questionId)
                .orElse(null);
    }

    @Override
    public boolean validateAnswer(Long questionId, Long answerId) {
        Answer answer = getAnswerById(answerId);
        return answer.getQuestion() != null &&
                answer.getQuestion().getId().equals(questionId) &&
                Boolean.TRUE.equals(answer.getIsCorrect());
    }

    @Override
    @Transactional
    public void recordAnswerSelection(Long quizId, Long questionId, Long answerId) {
        log.info("Recording answer selection - Quiz: {}, Question: {}, Answer: {}",
                quizId, questionId, answerId);
    }

    @Override
    public List<Answer> createAnswersBatch(List<AnswerDTO> answerDTOs) {
        log.info("Processing {} answers in batch", answerDTOs.size());

        List<Answer> answers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < answerDTOs.size(); i++) {
            AnswerDTO dto = answerDTOs.get(i);

            try {
                // Validation
                if (dto.getQuestionId() == null) {
                    errors.add("Answer " + i + ": Question ID is null");
                    continue;
                }

                if (dto.getText() == null || dto.getText().trim().isEmpty()) {
                    errors.add("Answer " + i + ": Text is empty");
                    continue;
                }

                if (dto.getIsCorrect() == null) {
                    errors.add("Answer " + i + ": isCorrect flag is null");
                    continue;
                }

                // Vérifier que la question existe
                Question question = questionRepository.findById(dto.getQuestionId())
                        .orElse(null);

                if (question == null) {
                    errors.add("Answer " + i + ": Question not found with id: " + dto.getQuestionId());
                    continue;
                }

                // Créer l'entité Answer
                Answer answer = new Answer();
                answer.setText(dto.getText().trim());
                answer.setIsCorrect(dto.getIsCorrect());
                answer.setExplanation(dto.getExplanation() != null ? dto.getExplanation()
                        : (dto.getIsCorrect() ? "Correct answer" : "Incorrect answer"));
                answer.setQuestion(question);

                answers.add(answer);

            } catch (Exception e) {
                errors.add("Answer " + i + ": " + e.getMessage());
                log.error("Error processing answer {}: {}", i, dto, e);
            }
        }

        if (!errors.isEmpty()) {
            log.error("Errors in batch processing: {}", errors);
        }

        if (answers.isEmpty()) {
            log.error("No valid answers to save. Errors: {}", errors);
            throw new RuntimeException("No valid answers to save: " + String.join(", ", errors));
        }

        log.info("Saving {} valid answers", answers.size());
        return answerRepository.saveAll(answers);
    }

    @Override
    @Transactional
    public void deleteAnswersByQuestionId(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            log.error("Question not found with id: {}", questionId);
            return;
        }
        List<Answer> answers = answerRepository.findByQuestionId(questionId);
        answerRepository.deleteAll(answers);
    }

    @Override
    public long getAnswerCountByQuestionId(Long questionId) {
        return answerRepository.countByQuestionId(questionId);
    }

    @Override
    public List<Answer> searchAnswers(String keyword) {
        return answerRepository.findByTextContainingIgnoreCase(keyword);
    }

    private void validateAnswer(Answer answer) {
        if (answer.getText() == null || answer.getText().trim().isEmpty()) {
            throw new IllegalArgumentException("Answer text cannot be empty");
        }
        if (answer.getQuestion() == null) {
            throw new IllegalArgumentException("Answer must be associated with a question");
        }
    }
}