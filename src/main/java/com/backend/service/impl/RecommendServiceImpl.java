package com.backend.service.impl;

import com.backend.common.BusinessException;
import com.backend.common.RedisKeys;
import com.backend.dto.PostVO;
import com.backend.dto.RecommendConfigUpdateRequest;
import com.backend.dto.RecommendConfigVO;
import com.backend.entity.Post;
import com.backend.entity.PostSimilarity;
import com.backend.entity.PostTag;
import com.backend.entity.User;
import com.backend.entity.UserBehavior;
import com.backend.mapper.PostMapper;
import com.backend.service.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendServiceImpl implements RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendServiceImpl.class);

    // === 默认召回数量配置 ===
    private static final int DEFAULT_TAG_MATCH_LIMIT = 500;
    private static final int DEFAULT_HOT_LIMIT = 500;
    private static final int DEFAULT_ITEM_CF_LIMIT = 200;

    // === 默认排序权重 ===
    private static final double DEFAULT_WEIGHT_HOT = 0.4;
    private static final double DEFAULT_WEIGHT_TAG = 0.3;
    private static final double DEFAULT_WEIGHT_FRESH = 0.2;
    private static final double DEFAULT_WEIGHT_DIVERSITY = 0.1;

    // === 已推荐衰减 ===
    private static final long SEEN_PENALTY_HOURS = 24;
    private static final double SEEN_PENALTY_WEIGHT = 1.2;
    private static final int MAX_SEEN_RECORDS = 200;

    @Value("${search.hybrid.default-semantic-weight:0.5}")
    private double defaultSemanticWeight;

    @Lazy
    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private PostTagService postTagService;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Autowired
    private PostSimilarityService postSimilarityService;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public IPage<PostVO> getFeed(Long userId, int page, int pageSize) {
        // 每次请求重新召回+排序，已看过的帖子通过 seen 降权自然排到后面
        List<Long> rankedIds = doRecommend(userId);

        int total = rankedIds.size();
        int from = (page - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Long> pagedIds = from < total ? rankedIds.subList(from, to) : Collections.emptyList();
        List<PostVO> vos = pagedIds.isEmpty()
                ? Collections.emptyList()
                : postService.listPostsByIds(pagedIds, userId);

        Page<PostVO> result = new Page<>(page, pageSize, total);
        result.setRecords(vos);

        // 记录已推荐过的帖子（24h内降权，下次"换一批"就不会再看到）
        if (userId != null && !pagedIds.isEmpty()) {
            recordSeenPosts(userId, pagedIds);
            // 全部帖子都推荐过时，重置惩罚从头开始
            Long seenCount = stringRedisTemplate.opsForZSet().zCard(RedisKeys.RECOMMEND_FEED + "seen:" + userId);
            if (seenCount != null && seenCount >= total) {
                stringRedisTemplate.delete(RedisKeys.RECOMMEND_FEED + "seen:" + userId);
            }
        }

        return result;
    }

    @Override
    public void reportBehavior(Long userId, Long postId, String actionType, Integer duration) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setPostId(postId);
        behavior.setActionType(actionType);
        behavior.setDuration(duration);
        userBehaviorService.save(behavior);
    }

    @Override
    public RecommendConfigVO getConfig() {
        return RecommendConfigVO.builder()
                .weightHot(getDoubleConfig(RedisKeys.RECOMMEND_WEIGHT_HOT, DEFAULT_WEIGHT_HOT))
                .weightTag(getDoubleConfig(RedisKeys.RECOMMEND_WEIGHT_TAG, DEFAULT_WEIGHT_TAG))
                .weightFresh(getDoubleConfig(RedisKeys.RECOMMEND_WEIGHT_FRESH, DEFAULT_WEIGHT_FRESH))
                .weightDiversity(getDoubleConfig(RedisKeys.RECOMMEND_WEIGHT_DIVERSITY, DEFAULT_WEIGHT_DIVERSITY))
                .tagMatchLimit(getIntConfig(RedisKeys.RECOMMEND_TAG_MATCH_LIMIT, DEFAULT_TAG_MATCH_LIMIT))
                .hotLimit(getIntConfig(RedisKeys.RECOMMEND_HOT_LIMIT, DEFAULT_HOT_LIMIT))
                .itemCfLimit(getIntConfig(RedisKeys.RECOMMEND_ITEM_CF_LIMIT, DEFAULT_ITEM_CF_LIMIT))
                .build();
    }

    @Override
    @Transactional
    public void updateConfig(Long adminUserId, RecommendConfigUpdateRequest request) {
        userService.checkAdminRole(adminUserId);
        if (request.getWeightHot() != null) {
            stringRedisTemplate.opsForValue().set(RedisKeys.RECOMMEND_WEIGHT_HOT, String.valueOf(request.getWeightHot()));
        }
        if (request.getWeightTag() != null) {
            stringRedisTemplate.opsForValue().set(RedisKeys.RECOMMEND_WEIGHT_TAG, String.valueOf(request.getWeightTag()));
        }
        if (request.getWeightFresh() != null) {
            stringRedisTemplate.opsForValue().set(RedisKeys.RECOMMEND_WEIGHT_FRESH, String.valueOf(request.getWeightFresh()));
        }
        if (request.getWeightDiversity() != null) {
            stringRedisTemplate.opsForValue().set(RedisKeys.RECOMMEND_WEIGHT_DIVERSITY, String.valueOf(request.getWeightDiversity()));
        }
        if (request.getTagMatchLimit() != null) {
            stringRedisTemplate.opsForValue().set(RedisKeys.RECOMMEND_TAG_MATCH_LIMIT, String.valueOf(request.getTagMatchLimit()));
        }
        if (request.getHotLimit() != null) {
            stringRedisTemplate.opsForValue().set(RedisKeys.RECOMMEND_HOT_LIMIT, String.valueOf(request.getHotLimit()));
        }
        if (request.getItemCfLimit() != null) {
            stringRedisTemplate.opsForValue().set(RedisKeys.RECOMMEND_ITEM_CF_LIMIT, String.valueOf(request.getItemCfLimit()));
        }
        stringRedisTemplate.opsForValue().set(RedisKeys.RECOMMEND_CONFIG_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        log.info("推荐配置已更新");
    }

    private double getDoubleConfig(String key, double defaultValue) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value != null && !value.isBlank()) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private int getIntConfig(String key, int defaultValue) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value != null && !value.isBlank()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    // ==================== 私有方法 ====================

    /**
     * 多路召回 + 综合排序。
     * 匿名用户仅使用热门召回。
     */
    private List<Long> doRecommend(Long userId) {
        int tagMatchLimit = getIntConfig(RedisKeys.RECOMMEND_TAG_MATCH_LIMIT, DEFAULT_TAG_MATCH_LIMIT);
        int hotLimit = getIntConfig(RedisKeys.RECOMMEND_HOT_LIMIT, DEFAULT_HOT_LIMIT);
        int itemCfLimit = getIntConfig(RedisKeys.RECOMMEND_ITEM_CF_LIMIT, DEFAULT_ITEM_CF_LIMIT);

        if (userId == null) {
            return hotRecall(hotLimit);
        }

        // 并行执行三路召回
        Map<Long, Double> tagScores = tagMatchRecall(userId, tagMatchLimit);
        Map<Long, Double> hotScores = hotRecallWithScore(hotLimit);
        Map<Long, Double> cfScores = itemCfRecall(userId, itemCfLimit);

        // 合并去重
        Set<Long> allIds = new HashSet<>();
        allIds.addAll(tagScores.keySet());
        allIds.addAll(hotScores.keySet());
        allIds.addAll(cfScores.keySet());

        if (allIds.isEmpty()) {
            log.warn("多路召回无结果，降级到热门: userId={}", userId);
            return hotRecall(DEFAULT_HOT_LIMIT);
        }

        return rank(allIds, tagScores, hotScores, cfScores, userId);
    }

    // ==================== 召回渠道 ====================

    /**
     * 标签匹配召回：根据用户偏好的标签，匹配包含这些标签的帖子。
     * 得分 = 匹配的标签数 / 用户偏好标签总数。
     */
    private Map<Long, Double> tagMatchRecall(Long userId, int limit) {
        User user = userService.getById(userId);
        if (user == null || user.getPreferredTags() == null || user.getPreferredTags().isBlank()) {
            return Collections.emptyMap();
        }

        List<Integer> preferredTagIds;
        try {
            preferredTagIds = objectMapper.readValue(user.getPreferredTags(), new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            log.warn("解析用户偏好标签失败: userId={}, tags={}", userId, user.getPreferredTags());
            return Collections.emptyMap();
        }

        if (preferredTagIds.isEmpty()) return Collections.emptyMap();

        // 查询包含这些标签的 post_tag 记录
        List<PostTag> postTags = postTagService.lambdaQuery()
                .in(PostTag::getTagId, preferredTagIds)
                .list();

        if (postTags.isEmpty()) return Collections.emptyMap();

        // 统计每个帖子匹配的标签数
        Map<Long, Integer> matchCount = new HashMap<>();
        for (PostTag pt : postTags) {
            matchCount.merge(pt.getPostId(), 1, Integer::sum);
        }

        double maxScore = preferredTagIds.size();
        Map<Long, Double> scores = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : matchCount.entrySet()) {
            scores.put(entry.getKey(), entry.getValue() / maxScore);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 热门召回（匿名用户直接返回 ID 列表）。
     */
    private List<Long> hotRecall(int limit) {
        List<Post> posts = postMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getHotScore)
                        .last("LIMIT " + limit));
        return posts.stream().map(Post::getPostId).collect(Collectors.toList());
    }

    /**
     * 热门召回带得分（用于个性化推荐融合）。
     */
    private Map<Long, Double> hotRecallWithScore(int limit) {
        List<Post> posts = postMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post>()
                        .eq(Post::getStatus, 1)
                        .orderByDesc(Post::getHotScore)
                        .last("LIMIT " + limit));

        if (posts.isEmpty()) return Collections.emptyMap();

        // 用 max hotScore 归一化
        double maxScore = posts.get(0).getHotScore() != null
                ? posts.get(0).getHotScore().doubleValue() : 1.0;
        if (maxScore <= 0) maxScore = 1.0;

        Map<Long, Double> scores = new LinkedHashMap<>();
        for (Post p : posts) {
            double s = p.getHotScore() != null ? p.getHotScore().doubleValue() / maxScore : 0.0;
            scores.put(p.getPostId(), s);
        }
        return scores;
    }

    /**
     * 协同过滤召回：从用户最近交互的帖子出发，找到相似的帖子。
     */
    private Map<Long, Double> itemCfRecall(Long userId, int limit) {
        // 用户最近交互的帖子（取最近 20 条）
        List<UserBehavior> recentBehaviors = userBehaviorService.lambdaQuery()
                .select(UserBehavior::getPostId)
                .eq(UserBehavior::getUserId, userId)
                .isNotNull(UserBehavior::getPostId)
                .orderByDesc(UserBehavior::getCreateTime)
                .last("LIMIT 20")
                .list();

        if (recentBehaviors.isEmpty()) return Collections.emptyMap();

        Set<Long> interactedPostIds = recentBehaviors.stream()
                .map(UserBehavior::getPostId)
                .collect(Collectors.toSet());

        // 通过 post_similarity 查询相似帖子（algorithm='item_cf'）
        List<PostSimilarity> similarities = postSimilarityService.lambdaQuery()
                .in(PostSimilarity::getPostId, interactedPostIds)
                .eq(PostSimilarity::getAlgorithm, "item_cf")
                .orderByDesc(PostSimilarity::getSimilarity)
                .last("LIMIT " + limit)
                .list();

        if (similarities.isEmpty()) return Collections.emptyMap();

        // 聚合：一个 similarPostId 可能来自多个源帖子，取最高分
        Map<Long, Double> scores = new LinkedHashMap<>();
        for (PostSimilarity ps : similarities) {
            if (interactedPostIds.contains(ps.getSimilarPostId())) continue;
            scores.merge(ps.getSimilarPostId(), ps.getSimilarity(), Double::max);
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    // ==================== 综合排序 ====================

    /**
     * 综合排序：
     *   score = weightHot × 热度 + weightTag × 标签匹配 + weightFresh × 新鲜度 + weightDiversity × 多样性
     */
    private List<Long> rank(Set<Long> allIds,
                            Map<Long, Double> tagScores,
                            Map<Long, Double> hotScores,
                            Map<Long, Double> cfScores,
                            Long userId) {
        double weightHot = getDoubleConfig(RedisKeys.RECOMMEND_WEIGHT_HOT, DEFAULT_WEIGHT_HOT);
        double weightTag = getDoubleConfig(RedisKeys.RECOMMEND_WEIGHT_TAG, DEFAULT_WEIGHT_TAG);
        double weightFresh = getDoubleConfig(RedisKeys.RECOMMEND_WEIGHT_FRESH, DEFAULT_WEIGHT_FRESH);
        double weightDiversity = getDoubleConfig(RedisKeys.RECOMMEND_WEIGHT_DIVERSITY, DEFAULT_WEIGHT_DIVERSITY);
        // 批量查询帖子信息
        List<Post> posts = postService.listByIds(new ArrayList<>(allIds));
        Map<Long, Post> postMap = posts.stream()
                .filter(p -> p.getStatus() == 1)
                .collect(Collectors.toMap(Post::getPostId, p -> p));

        // 读取已推荐过的帖子时间戳（用于降权）
        Map<Long, Long> seenTimestamps = userId != null ? getSeenTimestamps(userId) : Collections.emptyMap();
        long now = System.currentTimeMillis();

        double maxHot = hotScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxHot <= 0) maxHot = 1.0;
        double maxCf = cfScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxCf <= 0) maxCf = 1.0;

        // 第一轮：计算基础分
        List<RankItem> items = new ArrayList<>();
        for (Long postId : allIds) {
            Post post = postMap.get(postId);
            if (post == null) continue;

            double hotNorm = hotScores.getOrDefault(postId, 0.0) / maxHot;
            double tagNorm = tagScores.getOrDefault(postId, 0.0);
            double cfNorm = cfScores.getOrDefault(postId, 0.0) / maxCf;

            // 热度用 hotScore 或 cfScore（用户更可能对相似内容感兴趣）
            double hotComponent = Math.max(hotNorm, cfNorm);

            double freshScore = calcFreshness(post.getCreateTime());

            // 已推荐降权：S型曲线，初期缓慢衰减，12h衰减最快，24h后恢复
            double seenPenalty = 0;
            Long seenTime = seenTimestamps.get(postId);
            if (seenTime != null) {
                long hoursSinceSeen = (now - seenTime) / 3_600_000;
                if (hoursSinceSeen < SEEN_PENALTY_HOURS) {
                    seenPenalty = SEEN_PENALTY_WEIGHT / (1.0 + Math.exp(0.5 * (hoursSinceSeen - 12)));
                }
            }

            double baseScore = weightHot * hotComponent
                    + weightTag * tagNorm
                    + weightFresh * freshScore
                    - seenPenalty;

            items.add(new RankItem(postId, baseScore, post.getAuthorId()));
        }

        // 按基础分降序排列
        items.sort(Comparator.comparingDouble(RankItem::baseScore).reversed());

        // 第二轮：多样性加分（同一作者仅第一篇获得加分）
        Map<Long, Integer> authorCount = new HashMap<>();
        for (RankItem item : items) {
            int cnt = authorCount.merge(item.authorId(), 1, Integer::sum);
            double diversityBonus = cnt == 1 ? weightDiversity
                    : cnt == 2 ? weightDiversity * 0.5
                    : 0.0;
            item.finalScore = item.baseScore + diversityBonus;
        }

        // 按最终分降序排列
        items.sort(Comparator.comparingDouble(RankItem::finalScore).reversed());

        return items.stream()
                .map(RankItem::postId)
                .collect(Collectors.toList());
    }

    private static double calcFreshness(Date createTime) {
        if (createTime == null) return 0;
        LocalDate created = createTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        long days = ChronoUnit.DAYS.between(created, LocalDate.now());
        return 1.0 / (days + 1);
    }

    // ==================== 已推荐衰减 ====================

    private void recordSeenPosts(Long userId, List<Long> postIds) {
        try {
            String key = RedisKeys.RECOMMEND_FEED + "seen:" + userId;
            long now = System.currentTimeMillis();
            for (Long postId : postIds) {
                stringRedisTemplate.opsForZSet().add(key, postId.toString(), now);
            }
            stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, now - SEEN_PENALTY_HOURS * 3_600_000);
            Long size = stringRedisTemplate.opsForZSet().zCard(key);
            if (size != null && size > MAX_SEEN_RECORDS) {
                stringRedisTemplate.opsForZSet().removeRange(key, 0, size - MAX_SEEN_RECORDS - 1);
            }
        } catch (Exception e) {
            log.warn("记录已推荐帖子失败: {}", e.getMessage());
        }
    }

    private Map<Long, Long> getSeenTimestamps(Long userId) {
        try {
            String key = RedisKeys.RECOMMEND_FEED + "seen:" + userId;
            Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> tuples =
                    stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);
            if (tuples == null || tuples.isEmpty()) return Collections.emptyMap();

            Map<Long, Long> result = new HashMap<>();
            for (var tuple : tuples) {
                result.put(Long.valueOf(tuple.getValue()), tuple.getScore().longValue());
            }
            return result;
        } catch (Exception e) {
            log.warn("读取已推荐记录失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private static class RankItem {
        final Long postId;
        final double baseScore;
        final Long authorId;
        double finalScore;

        RankItem(Long postId, double baseScore, Long authorId) {
            this.postId = postId;
            this.baseScore = baseScore;
            this.authorId = authorId;
            this.finalScore = baseScore;
        }

        Long postId() { return postId; }
        double baseScore() { return baseScore; }
        Long authorId() { return authorId; }
        double finalScore() { return finalScore; }
    }
}
