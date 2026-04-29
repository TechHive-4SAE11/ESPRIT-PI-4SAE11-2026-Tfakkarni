package org.techhive.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "spring.cloud.config.import-check.enabled=false", "spring.cloud.config.fail-fast=false", "eureka.client.enabled=false", "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost"})
class ApiGatewayApplicationTests { @Test void contextLoads() {} }
