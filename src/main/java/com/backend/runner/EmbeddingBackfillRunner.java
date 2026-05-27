package com.backend.runner;

import com.backend.entity.Post;
import com.backend.entity.PostEmbedding;
import com.backend.service.EmbeddingService;
import com.backend.service.PostEmbeddingService;
import com.backend.service.PostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;



/**
 * 嵌入向量回填运行器
 * 
 * 注意：这不是系统运行的必要组件，主要用于以下场景：
 * 1. 系统启动时检查并补充缺失的帖子嵌入向量
 * 2. 在AI搜索功能启用后，为历史帖子生成向量表示
 * 3. 确保所有已审核帖子都有对应的嵌入向量用于相似度计算
 * 
 * 如果系统不需要基于向量的相似性搜索功能，可以安全地禁用此组件
 * 通过在配置文件中设置 embedding.backfill.enabled=false 来禁用
 */
@Component
@ConditionalOnProperty(value = "embedding.backfill.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddingBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingBackfillRunner.class);
    private static final String BACKFILL_LOCK_KEY = "embedding:backfill:lock";
    private static final String BACKFILL_PROGRESS_KEY = "embedding:backfill:progress";

    @Autowired
    private PostService postService;

    @Autowired
    private PostEmbeddingService postEmbeddingService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Value("${ai.siliconflow.embedding-model}")
    private String modelName;

    @Value("${search.backfill.batch-size:16}")
    private int batchSize;

    @Value("${search.backfill.batch-interval-ms:200}")
    private long batchIntervalMs;

    @Override
    public void run(String... args) {
        // 先检查是否有更紧急的启动任务（如审核锁清理），让出 CPU
        if (!tryAcquireLock()) {
            log.info("嵌入回填锁被其他实例持有，跳过");
            return;
        }

        try {
            long totalPosts = postService.count(new LambdaQueryWrapper<Post>().eq(Post::getStatus, 1));
            long embeddedCount = postEmbeddingService.count();
            long missing = totalPosts - embeddedCount;

            if (missing <= 0) {
                log.info("所有已审核帖子均已生成嵌入向量，无需回填 (total={})", totalPosts);
                return;
            }

            log.info("检测到 {} 个已审核帖子缺少嵌入向量，开始后台回填 (共 {} 篇)", missing, totalPosts);

            // 获取所有已有嵌入的帖子 ID
            Set<Long> embeddedIds = postEmbeddingService.list().stream()
                    .map(PostEmbedding::getPostId)
                    .collect(Collectors.toSet());

            // 分批获取缺失嵌入的帖子
            int offset = 0;
            int totalProcessed = 0;
            while (totalProcessed < missing) {
                var query = postService.lambdaQuery()
                        .eq(Post::getStatus, 1)
                        .orderByAsc(Post::getCreateTime);
                if (!embeddedIds.isEmpty()) {
                    query.notIn(Post::getPostId, embeddedIds);
                }
                List<Post> posts = query
                        .last("LIMIT " + batchSize + " OFFSET " + offset)
                        .list();

                if (posts.isEmpty()) break;

                List<String> texts = new ArrayList<>(posts.size());
                for (Post post : posts) {
                    texts.add(post.getTitle() + "\n" + post.getContent());
                }

                try {
                    List<float[]> vectors = embeddingService.embedBatch(texts);
                    for (int i = 0; i < posts.size(); i++) {
                        postEmbeddingService.saveEmbedding(posts.get(i).getPostId(), vectors.get(i), modelName);
                    }
                    totalProcessed += posts.size();
                    log.info("嵌入回填进度: {}/{}", totalProcessed, missing);

                    if (stringRedisTemplate != null) {
                        stringRedisTemplate.opsForValue().set(
                                BACKFILL_PROGRESS_KEY, String.valueOf(totalProcessed),
                                Duration.ofHours(1));
                    }
                } catch (Exception e) {
                    log.error("回填批次失败 (offset={}), 跳过该批: {}", offset, e.getMessage());
                }

                offset += batchSize;
                if (batchIntervalMs > 0 && totalProcessed < missing) {
                    Thread.sleep(batchIntervalMs);
                }
            }

            log.info("嵌入向量回填完成，共处理 {} 篇帖子", totalProcessed);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("嵌入回填被中断");
        } catch (Exception e) {
            log.error("嵌入回填异常: {}", e.getMessage());
        } finally {
            releaseLock();
        }
    }

    private boolean tryAcquireLock() {
        if (stringRedisTemplate == null) return true; // 无 Redis 时直接执行
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(BACKFILL_LOCK_KEY, "1", Duration.ofMinutes(30));
        return Boolean.TRUE.equals(acquired);
    }

    private void releaseLock() {
        if (stringRedisTemplate != null) {
            stringRedisTemplate.delete(BACKFILL_LOCK_KEY);
        }
    }
}
