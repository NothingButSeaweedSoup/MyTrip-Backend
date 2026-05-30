package com.backend.service.impl;

import com.backend.config.model.ModelRegistry;
import com.backend.dto.ModelConfigUpdateRequest;
import com.backend.dto.ModelConfigVO;
import com.backend.service.ModelConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;

@Service
public class ModelConfigServiceImpl implements ModelConfigService {

    private static final Logger log = LoggerFactory.getLogger(ModelConfigServiceImpl.class);

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Value("${app.model-config-path:conf/model-config.json}")
    private String configPath;

    @Value("${ai.deepseek.api-key:}")
    private String defaultDeepseekKey;

    @Value("${ai.vision.api-key:}")
    private String defaultVisionKey;

    @Value("${ai.siliconflow.api-key:}")
    private String defaultSiliconflowKey;

    @Autowired
    private ModelRegistry modelRegistry;

    private ModelConfigVO cached;

    @PostConstruct
    public void init() {
        cached = loadFromFile();
    }

    @Override
    public ModelConfigVO getConfig() {
        if (cached == null) {
            cached = loadFromFile();
        }
        return cached;
    }

    @Override
    public void updateConfig(ModelConfigUpdateRequest request) {
        ModelConfigVO vo = new ModelConfigVO();
        vo.setChat(request.getChat());
        vo.setReview(request.getReview());
        if (request.getVision() != null) {
            vo.setVision(request.getVision());
        }
        vo.setEmbedding(request.getEmbedding());

        try {
            Path path = resolvePath();
            Files.createDirectories(path.getParent());
            mapper.writeValue(path.toFile(), vo);
            cached = vo;
            log.info("模型配置已更新: {}", path);
            modelRegistry.reloadAll(vo);
        } catch (IOException e) {
            log.error("保存模型配置失败", e);
            throw new RuntimeException("保存模型配置失败", e);
        }
    }

    private ModelConfigVO loadFromFile() {
        Path path = resolvePath();
        if (!Files.exists(path)) {
            log.info("模型配置文件不存在，使用默认值: {}", path);
            return createDefault();
        }
        try {
            return mapper.readValue(path.toFile(), ModelConfigVO.class);
        } catch (IOException e) {
            log.error("读取模型配置失败，使用默认值", e);
            return createDefault();
        }
    }

    private Path resolvePath() {
        Path p = Paths.get(configPath);
        if (!p.isAbsolute()) {
            p = Paths.get("").toAbsolutePath().resolve(configPath);
        }
        return p;
    }

    private ModelConfigVO createDefault() {
        ModelConfigVO vo = new ModelConfigVO();

        ModelConfigVO.ModelItem chat = new ModelConfigVO.ModelItem();
        chat.setApiKey(defaultDeepseekKey);
        chat.setBaseUrl("https://api.deepseek.com/v1");
        chat.setModelName("deepseek-v4-flash");
        chat.setTemperature(0.2);
        chat.setMaxTokens(2000);
        chat.setTimeoutSeconds(60);
        vo.setChat(chat);

        ModelConfigVO.ModelItem review = new ModelConfigVO.ModelItem();
        review.setApiKey(defaultDeepseekKey);
        review.setBaseUrl("https://api.deepseek.com/v1");
        review.setModelName("deepseek-v4-flash");
        review.setTemperature(0.2);
        review.setMaxTokens(2000);
        review.setTimeoutSeconds(60);
        vo.setReview(review);

        ModelConfigVO.VisionModelItem vision = new ModelConfigVO.VisionModelItem();
        vision.setApiKey(defaultVisionKey);
        vision.setBaseUrl("https://api.xiaomimimo.com/v1");
        vision.setModelName("mimo-v2.5");
        vision.setTemperature(0.2);
        vision.setMaxTokens(2000);
        vision.setTimeoutSeconds(120);
        vision.setMaxImages(9);
        vo.setVision(vision);

        ModelConfigVO.EmbeddingModelItem emb = new ModelConfigVO.EmbeddingModelItem();
        emb.setApiKey(defaultSiliconflowKey);
        emb.setBaseUrl("https://api.siliconflow.cn/v1");
        emb.setModelName("BAAI/bge-m3");
        emb.setDimensions(384);
        emb.setBatchSize(16);
        vo.setEmbedding(emb);

        return vo;
    }
}
