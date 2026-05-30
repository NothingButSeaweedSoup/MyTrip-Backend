package com.backend.config.model;

import com.backend.dto.ModelConfigVO;
import dev.langchain4j.http.client.okhttp.OkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class ModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistry.class);

    private DynamicChatModel chatModel;
    private DynamicChatModel reviewChatModel;
    private DynamicChatModel visionChatModel;
    private DynamicStreamingChatModel streamingChatModel;
    private DynamicEmbeddingModel embeddingModel;

    public void registerChatModel(DynamicChatModel model) { this.chatModel = model; }
    public void registerReviewChatModel(DynamicChatModel model) { this.reviewChatModel = model; }
    public void registerVisionChatModel(DynamicChatModel model) { this.visionChatModel = model; }
    public void registerStreamingChatModel(DynamicStreamingChatModel model) { this.streamingChatModel = model; }
    public void registerEmbeddingModel(DynamicEmbeddingModel model) { this.embeddingModel = model; }

    public void reloadAll(ModelConfigVO cfg) {
        log.info("正在热更新模型配置...");

        if (chatModel != null && cfg.getChat() != null) {
            chatModel.swap(buildChatModel(cfg.getChat()));
            log.info("对话模型 已热更新");
        }

        if (reviewChatModel != null && cfg.getReview() != null) {
            reviewChatModel.swap(buildChatModel(cfg.getReview()));
            log.info("审核模型 已热更新");
        }

        if (visionChatModel != null && cfg.getVision() != null
                && cfg.getVision().getApiKey() != null && !cfg.getVision().getApiKey().isBlank()) {
            visionChatModel.swap(buildChatModel(cfg.getVision()));
            log.info("视觉模型 已热更新");
        }

        if (streamingChatModel != null && cfg.getChat() != null) {
            streamingChatModel.swap(buildStreamingChatModel(cfg.getChat()));
            log.info("流式对话模型 已热更新");
        }

        if (embeddingModel != null && cfg.getEmbedding() != null) {
            embeddingModel.swap(buildEmbeddingModel(cfg.getEmbedding()));
            log.info("嵌入模型 已热更新");
        }
    }

    private ChatModel buildChatModel(ModelConfigVO.ModelItem cfg) {
        return OpenAiChatModel.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(cfg.getApiKey())
                .modelName(cfg.getModelName())
                .temperature(cfg.getTemperature())
                .maxTokens(cfg.getMaxTokens())
                .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()))
                .maxRetries(1)
                .httpClientBuilder(new OkHttpClientBuilder())
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private StreamingChatModel buildStreamingChatModel(ModelConfigVO.ModelItem cfg) {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(cfg.getApiKey())
                .modelName(cfg.getModelName())
                .temperature(cfg.getTemperature())
                .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()))
                .httpClientBuilder(new OkHttpClientBuilder())
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    private EmbeddingModel buildEmbeddingModel(ModelConfigVO.EmbeddingModelItem cfg) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(cfg.getBaseUrl())
                .apiKey(cfg.getApiKey())
                .modelName(cfg.getModelName())
                .dimensions(cfg.getDimensions())
                .timeout(Duration.ofSeconds(120))
                .maxRetries(3)
                .httpClientBuilder(new OkHttpClientBuilder())
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
