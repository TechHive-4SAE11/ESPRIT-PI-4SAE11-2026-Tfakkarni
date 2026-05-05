package org.techhive.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.techhive.userservice.repository.UserRepository;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "keycloak.enabled=false",
    "keycloak.server-url=http://localhost:8080",
    "keycloak.realm=tfakkarni",
    "keycloak.admin.username=test-admin",
    "keycloak.admin.password=test-password",
    "keycloak.client-id=backend",
    "keycloak.client-secret=dummy-secret",
    "keycloak.frontend-client-id=frontend",
    "mailtrap.token=dummy-token",
    "mailtrap.inbox-id=dummy-inbox",
    "mailtrap.from=noreply@example.test",
    "didit.api-key=dummy-didit-key",
    "didit.workflow-id=dummy-workflow"
})
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
class UserServiceApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @Test
    void contextLoads() {
    }
}