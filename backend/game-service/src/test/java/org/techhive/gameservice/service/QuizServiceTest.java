package org.techhive.gameservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.techhive.gameservice.dto.QuizDTO;
import org.techhive.gameservice.entity.Quiz;
import org.techhive.gameservice.repository.QuizRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private IQuestionService questionService;

    @InjectMocks
    private IQuizServiceImp quizService;

    private Quiz sampleQuiz;
    private QuizDTO sampleQuizDTO;

    @BeforeEach
    void setUp() {
        sampleQuiz = new Quiz();
        sampleQuiz.setId(1L);
        sampleQuiz.setTopic("Memory Training");
        sampleQuiz.setTotalScore(80);
        sampleQuiz.setCaregiverId(10L);
        sampleQuiz.setDateTaken(LocalDateTime.now());
        sampleQuiz.setLevelReached(2);

        sampleQuizDTO = QuizDTO.builder()
                .id(1L)
                .topic("Memory Training")
                .totalScore(80)
                .caregiverId(10L)
                .dateTaken(LocalDateTime.now())
                .levelReached(2)
                .build();
    }

    @Test
    void createQuiz_withValidData_shouldReturnSavedQuiz() {
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);

        Quiz result = quizService.createQuiz(sampleQuizDTO);

        assertNotNull(result);
        assertEquals("Memory Training", result.getTopic());
        assertEquals(10L, result.getCaregiverId());
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void createQuiz_withNullTopic_shouldReturnNull() {
        QuizDTO invalidDTO = QuizDTO.builder()
                .topic(null)
                .caregiverId(10L)
                .build();

        Quiz result = quizService.createQuiz(invalidDTO);

        assertNull(result);
        verify(quizRepository, never()).save(any());
    }

    @Test
    void createQuiz_withNullCaregiverId_shouldReturnNull() {
        QuizDTO invalidDTO = QuizDTO.builder()
                .topic("Memory")
                .caregiverId(null)
                .build();

        Quiz result = quizService.createQuiz(invalidDTO);

        assertNull(result);
        verify(quizRepository, never()).save(any());
    }

    @Test
    void getQuizById_whenExists_shouldReturnQuiz() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));

        Quiz result = quizService.getQuizById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getQuizById_whenNotExists_shouldReturnNull() {
        when(quizRepository.findById(99L)).thenReturn(Optional.empty());

        Quiz result = quizService.getQuizById(99L);

        assertNull(result);
    }

    @Test
    void updateQuiz_whenExists_shouldReturnUpdatedQuiz() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);

        Quiz result = quizService.updateQuiz(sampleQuizDTO);

        assertNotNull(result);
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void updateQuiz_whenNotExists_shouldReturnNull() {
        when(quizRepository.findById(1L)).thenReturn(Optional.empty());

        Quiz result = quizService.updateQuiz(sampleQuizDTO);

        assertNull(result);
        verify(quizRepository, never()).save(any());
    }

    @Test
    void deleteQuiz_whenExists_shouldDelete() {
        when(quizRepository.existsById(1L)).thenReturn(true);

        quizService.deleteQuiz(1L);

        verify(quizRepository).deleteById(1L);
    }

    @Test
    void deleteQuiz_whenNotExists_shouldNotDelete() {
        when(quizRepository.existsById(99L)).thenReturn(false);

        quizService.deleteQuiz(99L);

        verify(quizRepository, never()).deleteById(any());
    }

    @Test
    void getQuizzesByCaregiverId_shouldReturnList() {
        when(quizRepository.findByCaregiverId(10L)).thenReturn(List.of(sampleQuiz));

        List<Quiz> result = quizService.getQuizzesByCaregiverId(10L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getCaregiverId());
    }

    @Test
    void searchQuizzesByTopic_withNullTopic_shouldReturnEmptyList() {
        List<Quiz> result = quizService.searchQuizzesByTopic(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void searchQuizzesByTopic_withValidTopic_shouldReturnResults() {
        when(quizRepository.findByTopicContainingIgnoreCase("Memory"))
                .thenReturn(List.of(sampleQuiz));

        List<Quiz> result = quizService.searchQuizzesByTopic("Memory");

        assertEquals(1, result.size());
    }

    @Test
    void startQuiz_whenExists_shouldResetScoreAndSetDate() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);

        Quiz result = quizService.startQuiz(1L);

        assertNotNull(result);
        assertEquals(0, result.getTotalScore());
        verify(quizRepository).save(any(Quiz.class));
    }

    @Test
    void completeQuiz_whenExists_shouldUpdateScore() {
        when(quizRepository.findById(1L)).thenReturn(Optional.of(sampleQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(sampleQuiz);

        Quiz result = quizService.completeQuiz(1L, 95, 3);

        assertNotNull(result);
        assertEquals(95, result.getTotalScore());
        assertEquals(3, result.getLevelReached());
    }

    @Test
    void getAverageScoreByCaregiver_withQuizzes_shouldReturnAverage() {
        Quiz quiz2 = new Quiz();
        quiz2.setTotalScore(60);
        quiz2.setCaregiverId(10L);

        when(quizRepository.findByCaregiverId(10L)).thenReturn(List.of(sampleQuiz, quiz2));

        double avg = quizService.getAverageScoreByCaregiver(10L);

        assertEquals(70.0, avg); // (80+60)/2
    }

    @Test
    void getAverageScoreByCaregiver_withNoQuizzes_shouldReturnZero() {
        when(quizRepository.findByCaregiverId(99L)).thenReturn(List.of());

        double avg = quizService.getAverageScoreByCaregiver(99L);

        assertEquals(0.0, avg);
    }

    @Test
    void getQuizCountByCaregiver_shouldReturnCount() {
        when(quizRepository.countByCaregiverId(10L)).thenReturn(5L);

        long count = quizService.getQuizCountByCaregiver(10L);

        assertEquals(5L, count);
    }
}
