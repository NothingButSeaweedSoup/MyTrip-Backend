package com.backend.config;

import dev.langchain4j.http.client.okhttp.OkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
public class AiConfig {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;

    @Value("${ai.deepseek.model-name}")
    private String modelName;

    @Value("${ai.vision.api-key:}")
    private String visionApiKey;

    @Value("${ai.vision.base-url:}")
    private String visionBaseUrl;

    @Value("${ai.vision.model-name:}")
    private String visionModelName;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .maxTokens(2000)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(1)
                .httpClientBuilder(new OkHttpClientBuilder())
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "ai.vision.api-key")
    public ChatModel visionChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(visionBaseUrl)
                .apiKey(visionApiKey)
                .modelName(visionModelName)
                .temperature(0.2)
                .maxTokens(2000)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(1)
                .httpClientBuilder(new OkHttpClientBuilder())
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(60))
                .httpClientBuilder(new OkHttpClientBuilder())
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
