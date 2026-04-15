package org.techhive.gameservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.AnswerDTO;
import org.techhive.gameservice.entity.Answer;
import org.techhive.gameservice.entity.Question;
import org.techhive.gameservice.repository.AnswerRepository;
import org.techhive.gameservice.repository.QuestionRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private IAnswerServiceImp answerService;

    private Question sampleQuestion;
    private Answer sampleAnswer;

    @BeforeEach
    void setUp() {
        sampleQuestion = new Question();
        sampleQuestion.setId(1L);
        sampleQuestion.setText("What is 2+2?");
        sampleQuestion.setDifficultyLevel(1);

        sampleAnswer = new Answer();
        sampleAnswer.setId(1L);
        sampleAnswer.setText("4");
        sampleAnswer.setIsCorrect(true);
        sampleAnswer.setExplanation("Basic arithmetic");
        sampleAnswer.setQuestion(sampleQuestion);
    }

    @Test
    void createAnswer_withValidData_shouldReturnSavedAnswer() {
        AnswerDTO dto = AnswerDTO.builder()
                .text("4")
                .isCorrect(true)
                .explanation("Basic arithmetic")
                .questionId(1L)
                .build();

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(answerRepository.save(any(Answer.class))).thenReturn(sampleAnswer);

        Answer result = answerService.createAnswer(dto);

        assertNotNull(result);
        assertEquals("4", result.getText());
        assertTrue(result.getIsCorrect());
        verify(answerRepository).save(any(Answer.class));
    }

    @Test
    void getAnswerById_whenExists_shouldReturnAnswer() {
        when(answerRepository.findById(1L)).thenReturn(Optional.of(sampleAnswer));

        Answer result = answerService.getAnswerById(1L);

        assertNotNull(result);
        assertEquals("4", result.getText());
    }

    @Test
    void updateAnswer_whenNotFound_shouldReturnNull() {
        AnswerDTO dto = AnswerDTO.builder().id(99L).text("test").questionId(1L).build();
        when(answerRepository.findById(99L)).thenReturn(Optional.empty());

        Answer result = answerService.updateAnswer(dto);

        assertNull(result);
        verify(answerRepository, never()).save(any());
    }

    @Test
    void deleteAnswer_whenExists_shouldDelete() {
        when(answerRepository.existsById(1L)).thenReturn(true);

        answerService.deleteAnswer(1L);

        verify(answerRepository).deleteById(1L);
    }

    @Test
    void getAnswersByQuestionId_shouldReturnList() {
        when(answerRepository.findByQuestionId(1L)).thenReturn(List.of(sampleAnswer));

        List<Answer> result = answerService.getAnswersByQuestionId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getCorrectAnswerByQuestionId_shouldReturnCorrectAnswer() {
        when(answerRepository.findByQuestionIdAndIsCorrectTrue(1L))
                .thenReturn(Optional.of(sampleAnswer));

        Answer result = answerService.getCorrectAnswerByQuestionId(1L);

        assertNotNull(result);
        assertTrue(result.getIsCorrect());
    }

    @Test
    void validateAnswer_withCorrectAnswer_shouldReturnTrue() {
        when(answerRepository.findById(1L)).thenReturn(Optional.of(sampleAnswer));

        boolean isValid = answerService.validateAnswer(1L, 1L);

        assertTrue(isValid);
    }

    @Test
    void validateAnswer_withIncorrectAnswer_shouldReturnFalse() {
        Answer wrongAnswer = new Answer();
        wrongAnswer.setId(2L);
        wrongAnswer.setIsCorrect(false);
        wrongAnswer.setQuestion(sampleQuestion);

        when(answerRepository.findById(2L)).thenReturn(Optional.of(wrongAnswer));

        boolean isValid = answerService.validateAnswer(1L, 2L);

        assertFalse(isValid);
    }

    @Test
    void createAnswersBatch_withValidData_shouldReturnSavedAnswers() {
        AnswerDTO dto1 = AnswerDTO.builder().text("A").isCorrect(true).questionId(1L).build();
        AnswerDTO dto2 = AnswerDTO.builder().text("B").isCorrect(false).questionId(1L).build();

        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));
        when(answerRepository.saveAll(anyList())).thenReturn(List.of(sampleAnswer, sampleAnswer));

        List<Answer> results = answerService.createAnswersBatch(List.of(dto1, dto2));

        assertEquals(2, results.size());
        verify(answerRepository).saveAll(anyList());
    }

    @Test
    void createAnswersBatch_withAllInvalid_shouldThrowException() {
        AnswerDTO dto = AnswerDTO.builder().text("").isCorrect(true).questionId(1L).build();

        assertThrows(RuntimeException.class, () ->
                answerService.createAnswersBatch(List.of(dto)));
    }

    @Test
    void getAnswerCountByQuestionId_shouldReturnCount() {
        when(answerRepository.countByQuestionId(1L)).thenReturn(4L);

        long count = answerService.getAnswerCountByQuestionId(1L);

        assertEquals(4L, count);
    }

    @Test
    void searchAnswers_shouldReturnMatchingAnswers() {
        when(answerRepository.findByTextContainingIgnoreCase("Paris"))
                .thenReturn(List.of(sampleAnswer));

        List<Answer> result = answerService.searchAnswers("Paris");

        assertEquals(1, result.size());
    }
}
