package com.backend.service.impl;

import com.backend.entity.ScenicSpotEmbedding;
import com.backend.mapper.ScenicSpotEmbeddingMapper;
import com.backend.service.EmbeddingService;
import com.backend.service.ScenicSpotEmbeddingService;
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
public class ScenicSpotEmbeddingServiceImpl extends ServiceImpl<ScenicSpotEmbeddingMapper, ScenicSpotEmbedding>
    implements ScenicSpotEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ScenicSpotEmbeddingServiceImpl.class);

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${ai.siliconflow.embedding-model}")
    private String modelName;

    @CacheEvict(value = "scenicSpotEmbeddings", allEntries = true)
    @Override
    public void saveEmbedding(Long spotId, float[] vector, String modelName) {
        ScenicSpotEmbedding record = new ScenicSpotEmbedding();
        record.setSpotId(spotId);
        record.setEmbedding(embeddingService.serialize(vector));
        record.setModelName(modelName);
        record.setUpdateTime(new Date());
        saveOrUpdate(record);
    }

    @Override
    public float[] getEmbedding(Long spotId) {
        ScenicSpotEmbedding record = getById(spotId);
        if (record == null) return null;
        return embeddingService.deserialize(record.getEmbedding());
    }

    @Override
    public Map<Long, Double> searchByVector(float[] queryVector, int topK) {
        List<VectorData> all = getCachedVectors();
        if (all.isEmpty()) return Collections.emptyMap();

        List<ScoredSpot> scored = all.parallelStream()
                .map(vd -> new ScoredSpot(vd.spotId, embeddingService.cosineSimilarity(queryVector, vd.vector)))
                .collect(Collectors.toList());

        scored.sort(Comparator.comparingDouble(ScoredSpot::score).reversed());
        LinkedHashMap<Long, Double> result = new LinkedHashMap<>();
        int limit = Math.min(topK, scored.size());
        for (int i = 0; i < limit; i++) {
            result.put(scored.get(i).spotId, scored.get(i).score);
        }
        return result;
    }

    @Cacheable("scenicSpotEmbeddings")
    public List<VectorData> getCachedVectors() {
        return list().stream()
                .map(se -> {
                    float[] vec = embeddingService.deserialize(se.getEmbedding());
                    return vec != null ? new VectorData(se.getSpotId(), vec) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private record VectorData(Long spotId, float[] vector) {}
    private record ScoredSpot(Long spotId, double score) {}
}
