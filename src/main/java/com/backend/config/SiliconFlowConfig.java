package com.backend.config;

import com.backend.config.model.DynamicEmbeddingModel;
import com.backend.config.model.ModelRegistry;
import com.backend.dto.ModelConfigVO;
import com.backend.service.ModelConfigService;
import dev.langchain4j.http.client.okhttp.OkHttpClientBuilder;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SiliconFlowConfig {

    @Autowired
    private ModelConfigService modelConfigService;

    @Autowired
    private ModelRegistry modelRegistry;

    @Bean
    public EmbeddingModel siliconFlowEmbeddingModel() {
        ModelConfigVO cfg = modelConfigService.getConfig();
        ModelConfigVO.EmbeddingModelItem emb = cfg.getEmbedding();

        OpenAiEmbeddingModel inner = OpenAiEmbeddingModel.builder()
                .baseUrl(emb.getBaseUrl())
                .apiKey(emb.getApiKey())
                .modelName(emb.getModelName())
                .dimensions(emb.getDimensions())
                .timeout(Duration.ofSeconds(120))
                .maxRetries(3)
                .httpClientBuilder(new OkHttpClientBuilder())
                .logRequests(false)
                .logResponses(false)
                .build();

        DynamicEmbeddingModel dynamic = new DynamicEmbeddingModel(inner);
        modelRegistry.registerEmbeddingModel(dynamic);
        return dynamic;
    }
}
