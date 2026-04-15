package org.techhive.gameservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "google.translate.api-key=test-key",
        "elevenlabs.api-key=test-key",
        "elevenlabs.voice-id-en=test-voice",
        "elevenlabs.voice-id-tn=test-voice",
        "elevenlabs.model-id=test-model"
})
class GameServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
