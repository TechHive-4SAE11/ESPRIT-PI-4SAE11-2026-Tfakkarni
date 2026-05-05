package org.techhive.discoveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "eureka.server.enable-self-preservation=false"
})
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    EurekaServerAutoConfiguration.class
})
class DiscoveryServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
