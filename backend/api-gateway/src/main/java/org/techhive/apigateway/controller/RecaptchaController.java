package org.techhive.apigateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class RecaptchaController {

    private final WebClient webClient = WebClient.create("https://www.google.com/recaptcha/api");

    @Value("${recaptcha.secret.key}")
    private String secretKey;

    @PostMapping("/verify-recaptcha")
    public Mono<ResponseEntity<Map<String, Object>>> verifyRecaptcha(@RequestParam("token") String token) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/siteverify")
                        .queryParam("secret", secretKey)
                        .queryParam("response", token)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mapResponse = (Map<String, Object>) response;
                    Boolean success = (Boolean) mapResponse.get("success");
                    if (Boolean.TRUE.equals(success)) {
                        return ResponseEntity.ok(mapResponse);
                    } else {
                        return ResponseEntity.badRequest().body(mapResponse);
                    }
                });
    }
}
