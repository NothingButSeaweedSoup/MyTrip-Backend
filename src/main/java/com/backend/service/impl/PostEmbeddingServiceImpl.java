package com.backend.service.impl;

import com.backend.entity.PostEmbedding;
import com.backend.mapper.PostEmbeddingMapper;
import com.backend.service.EmbeddingService;
import com.backend.service.PostEmbeddingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostEmbeddingServiceImpl extends ServiceImpl<PostEmbeddingMapper, PostEmbedding>
    implements PostEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(PostEmbeddingServiceImpl.class);

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${ai.siliconflow.embedding-model}")
    private String modelName;

    @CacheEvict(value = "postEmbeddings", allEntries = true)
    @Override
    public void saveEmbedding(Long postId, float[] vector, String modelName) {
        PostEmbedding record = new PostEmbedding();
        record.setPostId(postId);
        record.setEmbedding(embeddingService.serialize(vector));
        record.setModelName(modelName);
        record.setUpdateTime(new Date());
        saveOrUpdate(record);
    }

    @Override
    public float[] getEmbedding(Long postId) {
        PostEmbedding record = getById(postId);
        if (record == null) return null;
        return embeddingService.deserialize(record.getEmbedding());
    }

    @Override
    public List<Long> searchSimilar(Long targetId, int topK) {
        float[] targetVector = getEmbedding(targetId);
        if (targetVector == null) return Collections.emptyList();
        return new ArrayList<>(searchByVector(targetVector, topK).keySet());
    }

    @Override
    public Map<Long, Double> searchByVector(float[] queryVector, int topK) {
        List<VectorData> all = getCachedVectors();
        if (all.isEmpty()) return Collections.emptyMap();

        // 并行计算余弦相似度
        List<ScoredPost> scored = all.parallelStream()
                .map(vd -> new ScoredPost(vd.postId, embeddingService.cosineSimilarity(queryVector, vd.vector)))
                .collect(Collectors.toList());

        // 取 topK
        scored.sort(Comparator.comparingDouble(ScoredPost::score).reversed());
        LinkedHashMap<Long, Double> result = new LinkedHashMap<>();
        int limit = Math.min(topK, scored.size());
        for (int i = 0; i < limit; i++) {
            result.put(scored.get(i).postId, scored.get(i).score);
        }
        return result;
    }

    /** 缓存预反序列化的向量，避免每次搜索都做 byte[] → float[] */
    @Cacheable("postEmbeddings")
    public List<VectorData> getCachedVectors() {
        return list().stream()
                .map(pe -> {
                    float[] vec = embeddingService.deserialize(pe.getEmbedding());
                    return vec != null ? new VectorData(pe.getPostId(), vec) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private record VectorData(Long postId, float[] vector) {}
    private record ScoredPost(Long postId, double score) {}
}
