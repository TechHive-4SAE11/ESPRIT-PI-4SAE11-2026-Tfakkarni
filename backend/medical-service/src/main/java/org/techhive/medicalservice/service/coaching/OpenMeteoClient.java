package org.techhive.medicalservice.service.coaching;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * Open-Meteo — free, no API key. Used for outdoor coaching context only.
 */
@Slf4j
@Component
public class OpenMeteoClient {

    private static final String BASE = "https://api.open-meteo.com/v1/forecast";

    private final RestTemplate restTemplate;

    public OpenMeteoClient(@Qualifier("externalRestTemplate") RestTemplate externalRestTemplate) {
        this.restTemplate = externalRestTemplate;
    }
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${coaching.weather.enabled:true}")
    private boolean weatherEnabled;

    /**
     * One-line summary e.g. "18°C, code 1" for demo / context.
     */
    public Optional<String> fetchCurrentSummary(double latitude, double longitude) {
        if (!weatherEnabled) {
            return Optional.empty();
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE)
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("current", "temperature_2m,weather_code")
                    .queryParam("timezone", "auto")
                    .toUriString();
            String raw = restTemplate.getForObject(url, String.class);
            if (raw == null) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(raw);
            JsonNode current = root.path("current");
            if (current.isMissingNode()) {
                return Optional.empty();
            }
            double temp = current.path("temperature_2m").asDouble(Double.NaN);
            int code = current.path("weather_code").asInt(-1);
            if (!Double.isNaN(temp)) {
                return Optional.of(String.format("%.0f°C (code météo %d)", temp, code));
            }
        } catch (Exception e) {
            log.debug("Open-Meteo unavailable: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
