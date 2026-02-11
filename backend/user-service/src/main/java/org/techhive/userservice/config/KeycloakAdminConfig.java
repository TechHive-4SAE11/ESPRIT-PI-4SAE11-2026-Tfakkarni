package org.techhive.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class KeycloakAdminConfig {

  @Value("${keycloak.server-url}")
  private String serverUrl;

  @Value("${keycloak.realm}")
  private String realm;

  @Value("${keycloak.admin.username}")
  private String adminUsername;

  @Value("${keycloak.admin.password}")
  private String adminPassword;

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public String getRealm() {
    return realm;
  }

  public String getAdminUsername() {
    return adminUsername;
  }

  public String getAdminPassword() {
    return adminPassword;
  }
}
