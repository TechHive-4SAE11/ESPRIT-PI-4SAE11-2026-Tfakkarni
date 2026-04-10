package org.techhive.assistantservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "assistant.video")
public class VideoApiConfig {

    private String provider = "SCRIPT_ONLY";

    private DIdConfig dId = new DIdConfig();
    private HeyGenConfig heygen = new HeyGenConfig();
    private LumaConfig luma = new LumaConfig();
    private RunwayConfig runway = new RunwayConfig();

    @Data
    public static class DIdConfig {
        private String apiKey;
        private String apiUrl = "https://api.d-id.com";
    }

    @Data
    public static class HeyGenConfig {
        private String apiKey;
        private String apiUrl = "https://api.heygen.com";
    }

    @Data
    public static class LumaConfig {
        private String apiKey;
        private String apiUrl = "https://api.lumalabs.ai";
    }

    @Data
    public static class RunwayConfig {
        private String apiKey;
        private String apiUrl = "https://api.runwayml.com";
    }
}
