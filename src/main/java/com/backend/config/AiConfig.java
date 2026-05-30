package com.backend.config;

import com.backend.config.model.DynamicChatModel;
import com.backend.config.model.DynamicStreamingChatModel;
import com.backend.config.model.ModelRegistry;
import com.backend.dto.ModelConfigVO;
import com.backend.service.ModelConfigService;
import dev.langchain4j.http.client.okhttp.OkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

@Configuration
public class AiConfig {

    @Autowired
    private ModelConfigService modelConfigService;

    @Autowired
    private ModelRegistry modelRegistry;

    @Bean
    public ChatModel chatModel() {
        ModelConfigVO cfg = modelConfigService.getConfig();
        ModelConfigVO.ModelItem c = cfg.getChat();

        OpenAiChatModel inner = OpenAiChatModel.builder()
                .baseUrl(c.getBaseUrl())
                .apiKey(c.getApiKey())
                .modelName(c.getModelName())
                .temperature(c.getTemperature())
                .maxTokens(c.getMaxTokens())
                .timeout(Duration.ofSeconds(c.getTimeoutSeconds()))
                .maxRetries(1)
                .httpClientBuilder(new OkHttpClientBuilder())
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .logRequests(false)
                .logResponses(false)
                .build();

        DynamicChatModel dynamic = new DynamicChatModel(inner);
        modelRegistry.registerChatModel(dynamic);
        return dynamic;
    }

    @Bean
    public ChatModel reviewChatModel() {
        ModelConfigVO cfg = modelConfigService.getConfig();
        ModelConfigVO.ModelItem r = cfg.getReview();

        OpenAiChatModel inner = OpenAiChatModel.builder()
                .baseUrl(r.getBaseUrl())
                .apiKey(r.getApiKey())
                .modelName(r.getModelName())
                .temperature(r.getTemperature())
                .maxTokens(r.getMaxTokens())
                .timeout(Duration.ofSeconds(r.getTimeoutSeconds()))
                .maxRetries(1)
                .httpClientBuilder(new OkHttpClientBuilder())
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .logRequests(false)
                .logResponses(false)
                .build();

        DynamicChatModel dynamic = new DynamicChatModel(inner);
        modelRegistry.registerReviewChatModel(dynamic);
        return dynamic;
    }

    @Bean
    public ChatModel visionChatModel() {
        ModelConfigVO cfg = modelConfigService.getConfig();
        ModelConfigVO.VisionModelItem vis = cfg.getVision();

        OpenAiChatModel inner = OpenAiChatModel.builder()
                .baseUrl(vis.getBaseUrl())
                .apiKey(vis.getApiKey())
                .modelName(vis.getModelName())
                .temperature(vis.getTemperature())
                .maxTokens(vis.getMaxTokens())
                .timeout(Duration.ofSeconds(vis.getTimeoutSeconds()))
                .maxRetries(1)
                .httpClientBuilder(new OkHttpClientBuilder())
                .logRequests(false)
                .logResponses(false)
                .build();

        DynamicChatModel dynamic = new DynamicChatModel(inner);
        modelRegistry.registerVisionChatModel(dynamic);
        return dynamic;
    }

    @Bean
    public StreamingChatModel streamingChatModel() {
        ModelConfigVO cfg = modelConfigService.getConfig();
        ModelConfigVO.ModelItem c = cfg.getChat();

        OpenAiStreamingChatModel inner = OpenAiStreamingChatModel.builder()
                .baseUrl(c.getBaseUrl())
                .apiKey(c.getApiKey())
                .modelName(c.getModelName())
                .temperature(c.getTemperature())
                .timeout(Duration.ofSeconds(c.getTimeoutSeconds()))
                .httpClientBuilder(new OkHttpClientBuilder())
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .logRequests(false)
                .logResponses(false)
                .build();

        DynamicStreamingChatModel dynamic = new DynamicStreamingChatModel(inner);
        modelRegistry.registerStreamingChatModel(dynamic);
        return dynamic;
    }
}
