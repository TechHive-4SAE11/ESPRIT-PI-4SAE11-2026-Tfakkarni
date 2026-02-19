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
                answer.isCorrect();
    }

    @Override
    @Transactional
    public void recordAnswerSelection(Long quizId, Long questionId, Long answerId) {
        log.info("Recording answer selection - Quiz: {}, Question: {}, Answer: {}",
                quizId, questionId, answerId);
    }

    @Override
    public List<Answer> createAnswersBatch(List<AnswerDTO> answerDTOs) {
        List<Answer> answers = answerDTOs.stream()
                .map(dto -> {
                    Answer answer = dto.toEntity();
                    Question question = questionRepository.findById(dto.getQuestionId())
                            .orElse(null);
                    answer.setQuestion(question);
                    validateAnswer(answer);
                    return answer;
                })
                .toList();

        return (List<Answer>) answerRepository.saveAll(answers);
    }

    @Override
    @Transactional
    public void deleteAnswersByQuestionId(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            log.error("Question not found with id: {}", questionId);
            return;
        }
        answerRepository.deleteByQuestionId(questionId);
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