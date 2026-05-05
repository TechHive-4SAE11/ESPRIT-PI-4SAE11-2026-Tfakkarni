package org.techhive.assistantservice.exception;

import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleRuntimeException_shouldReturnInternalServerErrorBody() {
        ResponseEntity<Map<String, Object>> response = handler.handleRuntimeException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Internal Server Error", response.getBody().get("error"));
        assertEquals("boom", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleFeignException_shouldUseFeignStatusWhenResolvable() {
        feign.FeignException exception = new feign.FeignException.NotFound(
                "missing patient",
                request(),
                "not found".getBytes(StandardCharsets.UTF_8),
                Map.of());

        ResponseEntity<Map<String, Object>> response = handler.handleFeignException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Service Communication Error", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").toString().contains("Failed to communicate"));
    }

    @Test
    void handleFeignException_shouldFallbackTo500ForUnknownStatus() {
        feign.FeignException exception = new feign.FeignException(
                599,
                "custom downstream failure",
                request(),
                "custom".getBytes(StandardCharsets.UTF_8),
                Map.of()) {};

        ResponseEntity<Map<String, Object>> response = handler.handleFeignException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(599, response.getBody().get("status"));
        assertEquals("Service Communication Error", response.getBody().get("error"));
    }

    @Test
    void handleGenericException_shouldReturnUnexpectedErrorBody() {
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(new Exception("unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Unexpected Error", response.getBody().get("error"));
        assertEquals("unexpected", response.getBody().get("message"));
    }

    private Request request() {
        return Request.create(
                Request.HttpMethod.GET,
                "/patients/1",
                Map.of("Accept", List.of("application/json")),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate());
    }
}
