package org.techhive.mlservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "spring.ai.huggingface.api-key=dummy-key")
class MlServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
