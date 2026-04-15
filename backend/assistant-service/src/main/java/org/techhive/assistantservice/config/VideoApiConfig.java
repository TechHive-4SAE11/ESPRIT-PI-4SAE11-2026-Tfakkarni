package org.techhive.assistantservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "assistant.video")
public class VideoApiConfig {

    private String provider = "PEXELS";

    private PexelsConfig pexels = new PexelsConfig();

    @Data
    public static class PexelsConfig {
        private String apiKey;
        private String apiUrl = "https://api.pexels.com";
    }
}
