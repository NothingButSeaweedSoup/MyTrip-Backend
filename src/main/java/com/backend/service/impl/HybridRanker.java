package com.backend.service.impl;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合搜索融合排序器。
 * 将关键词得分和语义得分归一化后加权融合，按最终得分降序排列。
 */
@Component
public class HybridRanker {

    /**
     * 融合两组得分，返回按融合分降序排列的 ID 列表。
     *
     * @param keywordScores  关键词得分表 (可空)
     * @param semanticScores 语义得分表 (可空)
     * @param semanticWeight 语义权重 [0,1]
     */
    public <T> List<T> fuse(Map<T, Double> keywordScores,
                            Map<T, Double> semanticScores,
                            double semanticWeight) {
        Map<T, Double> kw = keywordScores != null ? keywordScores : Collections.emptyMap();
        Map<T, Double> sem = semanticScores != null ? semanticScores : Collections.emptyMap();

        Set<T> allIds = new HashSet<>();
        allIds.addAll(kw.keySet());
        allIds.addAll(sem.keySet());

        double kwMax = kw.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double semMax = sem.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        Comparator<ScoredId<T>> byScore = Comparator.comparingDouble(s -> s.score);
        return allIds.stream()
                .map(id -> {
                    double kwNorm = kw.getOrDefault(id, 0.0) / (kwMax > 0 ? kwMax : 1.0);
                    double semNorm = sem.getOrDefault(id, 0.0) / (semMax > 0 ? semMax : 1.0);
                    semNorm = Math.max(0, semNorm);
                    double finalScore = semanticWeight * semNorm + (1 - semanticWeight) * kwNorm;
                    return new ScoredId<>(id, finalScore);
                })
                .sorted(byScore.reversed())
                .map(ScoredId::id)
                .collect(Collectors.toList());
    }

    private record ScoredId<T>(T id, double score) {}
}
