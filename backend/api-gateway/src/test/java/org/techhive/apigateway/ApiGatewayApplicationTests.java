package org.techhive.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "keycloak.enabled=false",
    "recaptcha.secret.key=dummy-secret",
    "recaptcha.verify.url=http://localhost/recaptcha-test"
})
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
class ApiGatewayApplicationTests {

  @MockBean
  private ReactiveJwtDecoder reactiveJwtDecoder;

  @Test
  void contextLoads() {
  }
}
