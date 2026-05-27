package com.backend.service.impl;

import com.backend.dto.PostVO;
import com.backend.entity.Tag;
import com.backend.mapper.KeywordMatch;
import com.backend.mapper.PostMapper;
import com.backend.service.EmbeddingService;
import com.backend.service.PostEmbeddingService;
import com.backend.service.PostService;
import com.backend.service.SearchService;
import com.backend.service.TagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostService postService;

    @Autowired
    private TagService tagService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private PostEmbeddingService postEmbeddingService;

    @Autowired
    private HybridRanker hybridRanker;

    @Value("${search.hybrid.default-semantic-weight:0.5}")
    private double defaultSemanticWeight;

    @Override
    public IPage<PostVO> hybridSearch(String keyword, int page, int pageSize, Long userId, double semanticWeight) {
        Map<Long, Double> keywordScores = keywordSearchWithScore(keyword);
        Map<Long, Double> semanticScores = semanticSearchWithScore(keyword);

        if (semanticScores.isEmpty()) {
            log.warn("混合搜索：语义无结果，降级到关键词: keyword={}", keyword);
            return keywordSearch(keyword, page, pageSize, userId);
        }

        List<Long> sortedIds = hybridRanker.fuse(keywordScores, semanticScores, semanticWeight);
        return toPage(sortedIds, page, pageSize, userId);
    }

    @Override
    public IPage<PostVO> semanticSearch(String keyword, int page, int pageSize, Long userId) {
        Map<Long, Double> semanticScores = semanticSearchWithScore(keyword);

        if (semanticScores.isEmpty()) {
            log.warn("语义搜索无结果，降级到关键词: keyword={}", keyword);
            return keywordSearch(keyword, page, pageSize, userId);
        }

        List<Long> sortedIds = semanticScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return toPage(sortedIds, page, pageSize, userId);
    }

    @Override
    public IPage<PostVO> keywordSearch(String keyword, int page, int pageSize, Long userId) {
        Map<Long, Double> keywordScores = keywordSearchWithScore(keyword);
        List<Long> sortedIds = keywordScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return toPage(sortedIds, page, pageSize, userId);
    }

    @Override
    public List<String> suggest(String keyword) {
        return tagService.list(
                        new LambdaQueryWrapper<Tag>()
                                .like(Tag::getName, keyword)
                                .orderByDesc(Tag::getUseCount)
                                .last("LIMIT 10")
                ).stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
    }

    // ========== 私有方法 ==========

    private Map<Long, Double> keywordSearchWithScore(String keyword) {
        Map<Long, Double> scores = new HashMap<>();
        try {
            for (KeywordMatch m : postMapper.fulltextSearch(keyword)) {
                scores.merge(m.getPostId(), m.getScore(), Double::max);
            }
        } catch (Exception e) {
            log.debug("FULLTEXT 搜索失败，回退到 LIKE: {}", e.getMessage());
        }
        try {
            for (KeywordMatch m : postMapper.likeSearch(keyword)) {
                scores.merge(m.getPostId(), m.getScore(), Double::max);
            }
        } catch (Exception e) {
            log.warn("LIKE 搜索失败: {}", e.getMessage());
        }
        try {
            for (KeywordMatch m : postMapper.tagSearch(keyword)) {
                scores.merge(m.getPostId(), m.getScore(), Double::max);
            }
        } catch (Exception e) {
            log.warn("标签搜索失败: {}", e.getMessage());
        }
        return scores;
    }

    private Map<Long, Double> semanticSearchWithScore(String keyword) {
        try {
            float[] queryVec = embeddingService.embed(keyword);
            return postEmbeddingService.searchByVector(queryVec, 200);
        } catch (Exception e) {
            log.warn("语义搜索失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private IPage<PostVO> toPage(List<Long> sortedIds, int page, int pageSize, Long userId) {
        int total = sortedIds.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Long> pagedIds = from < total
                ? sortedIds.subList(from, to)
                : Collections.emptyList();
        List<PostVO> vos = pagedIds.isEmpty()
                ? Collections.emptyList()
                : postService.listPostsByIds(pagedIds, userId);

        Page<PostVO> result = new Page<>(page, pageSize, total);
        result.setRecords(vos);
        return result;
    }
}
