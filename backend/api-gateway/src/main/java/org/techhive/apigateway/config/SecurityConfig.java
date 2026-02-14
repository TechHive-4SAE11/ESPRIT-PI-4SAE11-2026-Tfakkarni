package org.techhive.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(exchanges -> exchanges
            // Public endpoints - no authentication required
            .pathMatchers("/public/**").permitAll()
            .pathMatchers("/actuator/**").permitAll()
            .pathMatchers("/api/users/register").permitAll()
            .pathMatchers("/api/games/play/**").permitAll()
            // All other endpoints require authentication
            .anyExchange().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(keycloakReactiveJwtAuthenticationConverter())));

    return http.build();
  }

  /**
   * Converts Keycloak realm_access.roles into Spring Security GrantedAuthorities
   * with a "ROLE_" prefix so they integrate with hasRole(...) checks.
   */
  private ReactiveJwtAuthenticationConverterAdapter keycloakReactiveJwtAuthenticationConverter() {
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
    return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
  }

  /**
   * Extracts roles from the Keycloak JWT token's realm_access.roles claim
   * and maps them to Spring Security GrantedAuthority objects.
   */
  static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

      if (realmAccess == null || realmAccess.isEmpty()) {
        return Collections.emptyList();
      }

      List<String> roles = (List<String>) realmAccess.get("roles");

      if (roles == null) {
        return Collections.emptyList();
      }

      return roles.stream()
          .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
          .collect(Collectors.toList());
    }
  }
}
