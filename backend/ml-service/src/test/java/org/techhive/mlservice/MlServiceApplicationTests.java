package org.techhive.mlservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.techhive.mlservice.repository.CaregiverStressHistoryRepository;
import org.techhive.mlservice.repository.ChatSessionRepository;
import org.techhive.mlservice.repository.ComplianceHistoryRepository;
import org.techhive.mlservice.repository.FAQAnalyticsRepository;
import org.techhive.mlservice.repository.TrainingModuleRepository;
import org.techhive.mlservice.repository.UserProgressRepository;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "keycloak.enabled=false",
    "spring.ai.huggingface.api-key=dummy-hf-key",
    "spring.ai.huggingface.chat.model=test-model"
})
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
class MlServiceApplicationTests {
    @MockBean
    private CaregiverStressHistoryRepository caregiverStressHistoryRepository;

    @MockBean
    private ChatSessionRepository chatSessionRepository;

    @MockBean
    private ComplianceHistoryRepository complianceHistoryRepository;

    @MockBean
    private FAQAnalyticsRepository fAQAnalyticsRepository;

    @MockBean
    private TrainingModuleRepository trainingModuleRepository;

    @MockBean
    private UserProgressRepository userProgressRepository;

    @Test
    void contextLoads() {
    }
}
