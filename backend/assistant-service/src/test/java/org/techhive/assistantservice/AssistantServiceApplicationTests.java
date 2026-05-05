package org.techhive.assistantservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.techhive.assistantservice.repository.GeneratedVideoRepository;
import org.techhive.assistantservice.repository.VideoFeedbackRepository;

@SpringBootTest
@ActiveProfiles("test")
class AssistantServiceApplicationTests {

    @MockBean
    private GeneratedVideoRepository generatedVideoRepository;

    @MockBean
    private VideoFeedbackRepository videoFeedbackRepository;

    @Test
    void contextLoads() {
    }
}
