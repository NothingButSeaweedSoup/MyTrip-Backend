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

    @Bean
    public EmbeddingModel siliconFlowEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(2)
                .httpClientBuilder(new OkHttpClientBuilder())
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
