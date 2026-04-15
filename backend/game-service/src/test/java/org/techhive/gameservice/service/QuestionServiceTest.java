package org.techhive.gameservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.QuestionDTO;
import org.techhive.gameservice.entity.Question;
import org.techhive.gameservice.entity.Quiz;
import org.techhive.gameservice.repository.QuestionRepository;
import org.techhive.gameservice.repository.QuizRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuizRepository quizRepository;

    @InjectMocks
    private IQestionServiceImp questionService;

    private Quiz sampleQuiz;
    private Question sampleQuestion;
    private QuestionDTO sampleQuestionDTO;

    @BeforeEach
    void setUp() {
        sampleQuiz = new Quiz();
        sampleQuiz.setId(1L);
        sampleQuiz.setTopic("Memory");
        sampleQuiz.setCaregiverId(10L);

        sampleQuestion = new Question();
        sampleQuestion.setId(1L);
        sampleQuestion.setText("What is the capital of France?");
        sampleQuestion.setDifficultyLevel(1);
        sampleQuestion.setQuiz(sampleQuiz);

        sampleQuestionDTO = QuestionDTO.builder()
                .id(1L)
                .text("What is the capital of France?")
                .difficultyLevel(1)
                .quizId(1L)
                .build();
    }

    @Test
    void createQuestion_withValidData_shouldReturnSavedQuestion() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(questionRepository.save(any(Question.class))).thenReturn(sampleQuestion);

        Question result = questionService.createQuestion(sampleQuestionDTO);

        assertNotNull(result);
        assertEquals("What is the capital of France?", result.getText());
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    void createQuestion_withNonExistentQuiz_shouldReturnNull() {
        when(quizRepository.findById(99L)).thenReturn(Optional.empty());

        QuestionDTO dto = QuestionDTO.builder()
                .text("Test question")
                .difficultyLevel(1)
                .quizId(99L)
                .build();

        Question result = questionService.createQuestion(dto);

        assertNull(result);
        verify(questionRepository, never()).save(any());
    }

    @Test
    void getQuestionById_whenExists_shouldReturnQuestion() {
        when(questionRepository.findById(1L)).thenReturn(Optional.of(sampleQuestion));

        Question result = questionService.getQuestionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getQuestionById_whenNotExists_shouldReturnNull() {
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        Question result = questionService.getQuestionById(99L);

        assertNull(result);
    }

    @Test
    void updateQuestion_whenQuestionNotFound_shouldReturnNull() {
        when(questionRepository.existsById(1L)).thenReturn(false);

        Question result = questionService.updateQuestion(sampleQuestionDTO);

        assertNull(result);
        verify(questionRepository, never()).save(any());
    }

    @Test
    void deleteQuestion_whenExists_shouldDelete() {
        when(questionRepository.existsById(1L)).thenReturn(true);

        questionService.deleteQuestion(1L);

        verify(questionRepository).deleteById(1L);
    }

    @Test
    void deleteQuestion_whenNotExists_shouldNotDelete() {
        when(questionRepository.existsById(99L)).thenReturn(false);

        questionService.deleteQuestion(99L);

        verify(questionRepository, never()).deleteById(any());
    }

    @Test
    void getQuestionsByQuizId_shouldReturnList() {
        when(questionRepository.findByQuizId(1L)).thenReturn(List.of(sampleQuestion));

        List<Question> result = questionService.getQuestionsByQuizId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void searchQuestions_withNullKeyword_shouldReturnEmptyList() {
        List<Question> result = questionService.searchQuestions(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void searchQuestions_withValidKeyword_shouldReturnResults() {
        when(questionRepository.findByTextContainingIgnoreCase("capital"))
                .thenReturn(List.of(sampleQuestion));

        List<Question> result = questionService.searchQuestions("capital");

        assertEquals(1, result.size());
    }

    @Test
    void calculateTotalPoints_shouldSumPointsByDifficulty() {
        Question q1 = new Question();
        q1.setDifficultyLevel(1);
        Question q2 = new Question();
        q2.setDifficultyLevel(3);

        when(questionRepository.findByQuizId(1L)).thenReturn(List.of(q1, q2));

        int totalPoints = questionService.calculateTotalPoints(1L);

        assertEquals(40, totalPoints); // (1*10) + (3*10)
    }

    @Test
    void validateQuestionDTO_withEmptyText_shouldReturnFalse() {
        QuestionDTO dto = QuestionDTO.builder()
                .text("")
                .difficultyLevel(1)
                .build();

        boolean valid = questionService.validateQuestion(dto);

        assertFalse(valid);
    }

    @Test
    void validateQuestionDTO_withValidData_shouldReturnTrue() {
        QuestionDTO dto = QuestionDTO.builder()
                .text("Valid question?")
                .difficultyLevel(2)
                .build();

        boolean valid = questionService.validateQuestion(dto);

        assertTrue(valid);
    }
}
