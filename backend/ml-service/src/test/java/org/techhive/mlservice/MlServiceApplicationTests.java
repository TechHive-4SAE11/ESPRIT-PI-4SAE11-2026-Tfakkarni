package org.techhive.mlservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.techhive.mlservice.repository.CaregiverStressHistoryRepository;
import org.techhive.mlservice.repository.ChatSessionRepository;
import org.techhive.mlservice.repository.ComplianceHistoryRepository;
import org.techhive.mlservice.repository.FAQAnalyticsRepository;
import org.techhive.mlservice.repository.TrainingModuleRepository;
import org.techhive.mlservice.repository.UserProgressRepository;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.huggingface.api-key=dummy-key",
        "huggingface.api-key=dummy-key",
        "gemini.api-key=dummy-key",
        "spring.datasource.url=jdbc:h2:mem:ml_service_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
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
