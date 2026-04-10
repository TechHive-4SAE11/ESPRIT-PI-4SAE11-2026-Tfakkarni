package org.techhive.mlservice.config;

import org.springframework.ai.huggingface.HuggingfaceChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HuggingFaceConfig {

    @Value("${spring.ai.huggingface.api-key:}")
    private String apiKey;

    @Value("${spring.ai.huggingface.chat.model:meta-llama/Llama-2-7b-chat-hf}")
    private String model;

    @Bean
    public HuggingfaceChatClient huggingfaceChatClient() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("HuggingFace API key is required");
        }
        String url = "https://api-inference.huggingface.co/models/" + model;
        return new HuggingfaceChatClient(apiKey, url);
    }
}