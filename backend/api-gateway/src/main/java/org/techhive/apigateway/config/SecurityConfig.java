package org.techhive.apigateway.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "http://localhost:5173",
        "http://127.0.0.1:5173"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(
        List.of("Authorization", "Content-Type", "X-User-Id", "Accept", "Origin", "X-Requested-With"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
        .authorizeExchange(exchanges -> exchanges
            // Allow all CORS preflight requests
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // Public endpoints - no authentication required
            .pathMatchers("/public/**").permitAll()
            .pathMatchers("/actuator/**").permitAll()
            .pathMatchers("/api/users/register").permitAll()
            .pathMatchers("/api/password-reset/**").permitAll()
            .pathMatchers("/api/users/kyc/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/api/users/keycloak/**").permitAll()
            .pathMatchers("/api/games/play/**").permitAll()
            .pathMatchers("/api/games/movies/play/**").permitAll()
            // Admin health check endpoint (for monitoring/testing)
            .pathMatchers("/api/admin/medication-status/health").permitAll()
            // IoT endpoints (heartbeat ingestion + sleep analysis)
            .pathMatchers("/api/iot/**").permitAll()
            // Public quiz endpoints
            .pathMatchers(HttpMethod.GET, "/api/games/quiz/1").permitAll()
            .pathMatchers(HttpMethod.GET, "/api/games/quiz/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/api/games/quiz/questions/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/api/games/quiz/answer/**").permitAll()
            
            // Medical folder and Patient analytics restricted to Doctors
            .pathMatchers("/api/medical-folders/analytics/**").hasAnyRole("DOCTOR","ADMIN")
            .pathMatchers("/api/analytics/patient/**").permitAll()
            .pathMatchers("/api/analytics/safety-audit/**").hasRole("DOCTOR")
            .pathMatchers("/api/games/stats/analytics/**").permitAll()
            .pathMatchers("/api/games/stats/overview").permitAll()
            
            // Prescription & Care Plan - Only Doctors/Admins can modify, Patients can only view
            .pathMatchers(HttpMethod.POST, "/api/prescriptions/**").hasRole("DOCTOR")
            .pathMatchers(HttpMethod.PUT, "/api/prescriptions/**").hasRole("DOCTOR")
            .pathMatchers(HttpMethod.PATCH, "/api/prescriptions/**").hasRole("DOCTOR")
            .pathMatchers(HttpMethod.DELETE, "/api/prescriptions/**").hasRole("DOCTOR")
            .pathMatchers(HttpMethod.GET, "/api/prescriptions/**").hasAnyRole("DOCTOR", "PATIENT", "ADMIN")
            
            .pathMatchers(HttpMethod.POST, "/api/care-plans/**").hasRole("DOCTOR")
            .pathMatchers(HttpMethod.PUT, "/api/care-plans/**").hasRole("DOCTOR")
            .pathMatchers(HttpMethod.PATCH, "/api/care-plans/**").hasRole("DOCTOR")
            .pathMatchers(HttpMethod.DELETE, "/api/care-plans/**").hasRole("DOCTOR")
            .pathMatchers(HttpMethod.GET, "/api/care-plans/**").hasAnyRole("DOCTOR", "PATIENT", "ADMIN")
            
            // Medication management restricted to Doctors or Admins for modification
            .pathMatchers(HttpMethod.POST, "/api/medications/**").hasAnyRole("DOCTOR", "ADMIN")
            .pathMatchers(HttpMethod.PUT, "/api/medications/**").hasAnyRole("DOCTOR", "ADMIN")
            .pathMatchers(HttpMethod.PATCH, "/api/medications/**").hasAnyRole("DOCTOR", "ADMIN")
            .pathMatchers(HttpMethod.DELETE, "/api/medications/**").hasAnyRole("DOCTOR", "ADMIN")
            .pathMatchers(HttpMethod.GET, "/api/medications/**").hasAnyRole("DOCTOR", "PATIENT", "ADMIN")
            
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
