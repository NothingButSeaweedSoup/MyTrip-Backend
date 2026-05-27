package com.backend.service.impl;

import com.backend.entity.Post;
import com.backend.service.EmbeddingService;
import com.backend.service.PostEmbeddingService;
import com.backend.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PostEmbeddingGenerator {

    private static final Logger log = LoggerFactory.getLogger(PostEmbeddingGenerator.class);

    @Lazy
    @Autowired
    private PostService postService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private PostEmbeddingService postEmbeddingService;

    @Value("${ai.siliconflow.embedding-model}")
    private String modelName;

    @Async("embeddingExecutor")
    public void generateEmbeddingAsync(Long postId) {
        try {
            Post post = postService.getById(postId);
            if (post == null) return;
            String text = post.getTitle() + "\n" + post.getContent();
            float[] vector = embeddingService.embed(text);
            postEmbeddingService.saveEmbedding(postId, vector, modelName);
            log.info("帖子 {} 嵌入生成完成", postId);
        } catch (Exception e) {
            log.error("帖子 {} 嵌入生成失败: {}", postId, e.getMessage());
        }
    }
}
