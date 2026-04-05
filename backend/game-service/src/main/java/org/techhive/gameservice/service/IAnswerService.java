package org.techhive.gameservice.service;

import org.techhive.gameservice.dto.AnswerDTO;
import org.techhive.gameservice.entity.Answer;

import java.util.List;

public interface IAnswerService {

    // Basic CRUD
    Answer createAnswer(AnswerDTO answerDTO);
    Answer getAnswerById(long id);
    Answer updateAnswer(AnswerDTO answerDTO);
    void deleteAnswer(long id);
    List<Answer> getAllAnswers();

    // New methods needed
    List<Answer> getAnswersByQuestionId(Long questionId);
    Answer getCorrectAnswerByQuestionId(Long questionId);
    boolean validateAnswer(Long questionId, Long answerId);
    void recordAnswerSelection(Long quizId, Long questionId, Long answerId);
    List<Answer> createAnswersBatch(List<AnswerDTO> answerDTOs);
    void deleteAnswersByQuestionId(Long questionId);
    long getAnswerCountByQuestionId(Long questionId);
    List<Answer> searchAnswers(String keyword);
}
