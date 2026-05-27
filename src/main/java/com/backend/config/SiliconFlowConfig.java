package com.backend.config;

import dev.langchain4j.http.client.okhttp.OkHttpClientBuilder;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SiliconFlowConfig {

    @Value("${ai.siliconflow.api-key}")
    private String apiKey;

    @Value("${ai.siliconflow.base-url}")
    private String baseUrl;

    @Value("${ai.siliconflow.embedding-model}")
    private String embeddingModel;

    @Value("${ai.siliconflow.embedding-dimensions:256}")
    private int embeddingDimensions;

    @Bean
    public EmbeddingModel siliconFlowEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .dimensions(embeddingDimensions)
                .timeout(Duration.ofSeconds(120))
                .maxRetries(3)
                .httpClientBuilder(new OkHttpClientBuilder())
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
