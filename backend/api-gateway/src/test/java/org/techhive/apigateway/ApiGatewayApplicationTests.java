package org.techhive.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@SpringBootTest(properties = {
    "recaptcha.secret.key=test-recaptcha-secret"
})
class ApiGatewayApplicationTests {

  @MockBean
  private ReactiveJwtDecoder reactiveJwtDecoder;

  @Test
  void contextLoads() {
  }
}
