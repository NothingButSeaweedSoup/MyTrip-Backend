package com.backend.service.impl;

import com.backend.dto.ScenicSpotVO;
import com.backend.entity.ScenicSpot;
import com.backend.mapper.ScenicSpotMapper;
import com.backend.service.EmbeddingService;
import com.backend.service.ScenicSpotEmbeddingService;
import com.backend.service.ScenicSpotSearchService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScenicSpotSearchServiceImpl implements ScenicSpotSearchService {

    private static final Logger log = LoggerFactory.getLogger(ScenicSpotSearchServiceImpl.class);

    @Autowired
    private ScenicSpotMapper scenicSpotMapper;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private ScenicSpotEmbeddingService scenicSpotEmbeddingService;

    @Autowired
    private HybridRanker hybridRanker;

    @Override
    public IPage<ScenicSpotVO> search(String keyword, int page, int pageSize, double semanticWeight) {
        Map<Long, Double> kwScores = keywordSearchWithScore(keyword);
        Map<Long, Double> semScores = semanticSearchWithScore(keyword);

        if (semScores.isEmpty()) {
            log.warn("景点混合搜索：语义无结果，仅用关键词: keyword={}", keyword);
            return keywordSearch(keyword, page, pageSize);
        }

        List<Long> sortedIds = hybridRanker.fuse(kwScores, semScores, semanticWeight);
        return toPage(sortedIds, page, pageSize);
    }

    @Override
    public IPage<ScenicSpotVO> semanticSearch(String keyword, int page, int pageSize) {
        Map<Long, Double> semScores = semanticSearchWithScore(keyword);
        if (semScores.isEmpty()) {
            log.warn("景点语义搜索无结果，降级到关键词: keyword={}", keyword);
            return keywordSearch(keyword, page, pageSize);
        }
        List<Long> sortedIds = semScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return toPage(sortedIds, page, pageSize);
    }

    @Override
    public IPage<ScenicSpotVO> keywordSearch(String keyword, int page, int pageSize) {
        Map<Long, Double> kwScores = keywordSearchWithScore(keyword);
        List<Long> sortedIds = kwScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return toPage(sortedIds, page, pageSize);
    }

    private Map<Long, Double> keywordSearchWithScore(String keyword) {
        try {
            List<ScenicSpot> results = scenicSpotMapper.keywordSearch(keyword);
            Map<Long, Double> scores = new HashMap<>();
            for (ScenicSpot s : results) {
                scores.put(s.getSpotId(), 1.0);
            }
            return scores;
        } catch (Exception e) {
            log.warn("景点关键词搜索失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, Double> semanticSearchWithScore(String keyword) {
        try {
            float[] queryVec = embeddingService.embed(keyword);
            return scenicSpotEmbeddingService.searchByVector(queryVec, 200);
        } catch (Exception e) {
            log.warn("景点语义搜索失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private IPage<ScenicSpotVO> toPage(List<Long> sortedIds, int page, int pageSize) {
        int total = sortedIds.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Long> pagedIds = from < total ? sortedIds.subList(from, to) : Collections.emptyList();

        List<ScenicSpotVO> vos;
        if (pagedIds.isEmpty()) {
            vos = Collections.emptyList();
        } else {
            List<ScenicSpot> spots = scenicSpotMapper.selectBatchIds(pagedIds);
            Map<Long, ScenicSpot> spotMap = spots.stream()
                    .collect(Collectors.toMap(ScenicSpot::getSpotId, s -> s));
            vos = pagedIds.stream()
                    .map(spotMap::get)
                    .filter(Objects::nonNull)
                    .map(this::toScenicSpotVO)
                    .collect(Collectors.toList());
        }

        Page<ScenicSpotVO> result = new Page<>(page, pageSize, total);
        result.setRecords(vos);
        return result;
    }

    private ScenicSpotVO toScenicSpotVO(ScenicSpot spot) {
        List<String> tagList = Collections.emptyList();
        if (spot.getTags() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> parsed = (List<String>) spot.getTags();
                tagList = parsed;
            } catch (Exception ignored) {}
        }
        return ScenicSpotVO.builder()
                .spotId(spot.getSpotId())
                .name(spot.getName())
                .city(spot.getCity())
                .address(spot.getAddress())
                .latitude(spot.getLatitude())
                .longitude(spot.getLongitude())
                .description(spot.getDescription())
                .tags(tagList)
                .rating(spot.getRating())
                .visitDuration(spot.getVisitDuration())
                .openTime(spot.getOpenTime())
                .phone(spot.getPhone())
                .coverImage(spot.getCoverImage())
                .build();
    }
}
