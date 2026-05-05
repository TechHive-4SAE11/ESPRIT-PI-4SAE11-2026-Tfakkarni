package org.techhive.gameservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.techhive.gameservice.repository.AnswerRepository;
import org.techhive.gameservice.repository.CustomGameAttemptRepository;
import org.techhive.gameservice.repository.CustomGameRepository;
import org.techhive.gameservice.repository.DataPointPerformanceRepository;
import org.techhive.gameservice.repository.GameAttemptRepository;
import org.techhive.gameservice.repository.GameImageRepository;
import org.techhive.gameservice.repository.MemoryPlaceRepository;
import org.techhive.gameservice.repository.MemoryTagRepository;
import org.techhive.gameservice.repository.MiniGameRepository;
import org.techhive.gameservice.repository.MovieGameAttemptRepository;
import org.techhive.gameservice.repository.MovieGameItemRepository;
import org.techhive.gameservice.repository.MovieGameRepository;
import org.techhive.gameservice.repository.MovieMemoryRepository;
import org.techhive.gameservice.repository.PersonalQuestionAttemptRepository;
import org.techhive.gameservice.repository.PersonalQuestionGameRepository;
import org.techhive.gameservice.repository.PersonalQuestionItemRepository;
import org.techhive.gameservice.repository.PhotoMemoryRepository;
import org.techhive.gameservice.repository.PlaceMemoryRepository;
import org.techhive.gameservice.repository.QuestionMemoryRepository;
import org.techhive.gameservice.repository.QuestionRepository;
import org.techhive.gameservice.repository.QuizRepository;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "google.translate.api-key=test-key",
        "elevenlabs.api-key=test-key",
        "elevenlabs.voice-id-en=test-voice",
        "elevenlabs.voice-id-tn=test-voice",
        "elevenlabs.model-id=test-model"
})
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
class GameServiceApplicationTests {

    @MockBean
    private AnswerRepository answerRepository;

    @MockBean
    private CustomGameAttemptRepository customGameAttemptRepository;

    @MockBean
    private CustomGameRepository customGameRepository;

    @MockBean
    private DataPointPerformanceRepository dataPointPerformanceRepository;

    @MockBean
    private GameAttemptRepository gameAttemptRepository;

    @MockBean
    private GameImageRepository gameImageRepository;

    @MockBean
    private MemoryPlaceRepository memoryPlaceRepository;

    @MockBean
    private MemoryTagRepository memoryTagRepository;

    @MockBean
    private MiniGameRepository miniGameRepository;

    @MockBean
    private MovieGameAttemptRepository movieGameAttemptRepository;

    @MockBean
    private MovieGameItemRepository movieGameItemRepository;

    @MockBean
    private MovieGameRepository movieGameRepository;

    @MockBean
    private MovieMemoryRepository movieMemoryRepository;

    @MockBean
    private PersonalQuestionAttemptRepository personalQuestionAttemptRepository;

    @MockBean
    private PersonalQuestionGameRepository personalQuestionGameRepository;

    @MockBean
    private PersonalQuestionItemRepository personalQuestionItemRepository;

    @MockBean
    private PhotoMemoryRepository photoMemoryRepository;

    @MockBean
    private PlaceMemoryRepository placeMemoryRepository;

    @MockBean
    private QuestionMemoryRepository questionMemoryRepository;

    @MockBean
    private QuestionRepository questionRepository;

    @MockBean
    private QuizRepository quizRepository;

    @Test
    void contextLoads() {
    }
}
