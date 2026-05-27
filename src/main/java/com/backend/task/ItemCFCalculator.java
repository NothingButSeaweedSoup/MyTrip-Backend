package com.backend.task;

import com.backend.entity.PostSimilarity;
import com.backend.entity.UserBehavior;
import com.backend.mapper.PostSimilarityMapper;
import com.backend.service.PostSimilarityService;
import com.backend.service.UserBehaviorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 协同过滤离线计算（Item-Based Collaborative Filtering）。
 *
 * 每天凌晨 3 点执行，基于 user_behavior 表计算帖子间的相似度，
 * 将 top 20 最相似帖子写入 post_similarity 表。
 *
 * 算法：
 *   sim(i,j) = |U(i) ∩ U(j)| / sqrt(|U(i)| × |U(j)|)
 *   U(i) = 与帖子 i 有过有效交互的用户集合
 *
 * 行为权重：like / comment 计入，view 仅 duration > 30s 计入。
 */
@Component
public class ItemCFCalculator {

    private static final Logger log = LoggerFactory.getLogger(ItemCFCalculator.class);

    private static final int TOP_K = 20;
    private static final int MIN_USER_POSTS = 2;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Autowired
    private PostSimilarityService postSimilarityService;

    @Autowired
    private PostSimilarityMapper postSimilarityMapper;

    @Scheduled(cron = "0 0 3 * * *")
    public void calculateItemCF() {
        long start = System.currentTimeMillis();
        log.info("ItemCF 协同过滤计算开始");

        try {
            Map<Long, Set<Long>> userPosts = loadUserPosts();
            log.info("加载 {} 个用户的交互数据", userPosts.size());

            Map<Long, Integer> postUserCount = new HashMap<>();
            for (Set<Long> posts : userPosts.values()) {
                for (Long postId : posts) {
                    postUserCount.merge(postId, 1, Integer::sum);
                }
            }
            log.info("统计到 {} 个帖子有交互", postUserCount.size());

            if (postUserCount.isEmpty()) {
                log.warn("无有效交互数据，跳过 ItemCF 计算");
                return;
            }

            Map<Long, Map<Long, Integer>> coCountMap = new HashMap<>();
            for (Set<Long> posts : userPosts.values()) {
                if (posts.size() < MIN_USER_POSTS) continue;
                List<Long> list = new ArrayList<>(posts);
                for (int i = 0; i < list.size(); i++) {
                    Long a = list.get(i);
                    for (int j = i + 1; j < list.size(); j++) {
                        Long b = list.get(j);
                        coCountMap.computeIfAbsent(a, k -> new HashMap<>()).merge(b, 1, Integer::sum);
                        coCountMap.computeIfAbsent(b, k -> new HashMap<>()).merge(a, 1, Integer::sum);
                    }
                }
            }

            List<PostSimilarity> allSimilarities = new ArrayList<>();
            for (Map.Entry<Long, Map<Long, Integer>> entry : coCountMap.entrySet()) {
                Long postId = entry.getKey();
                int uA = postUserCount.getOrDefault(postId, 0);
                if (uA == 0) continue;

                List<SimilarCandidate> candidates = new ArrayList<>();
                for (Map.Entry<Long, Integer> ce : entry.getValue().entrySet()) {
                    Long similarPostId = ce.getKey();
                    int coCount = ce.getValue();
                    int uB = postUserCount.getOrDefault(similarPostId, 0);
                    if (uB == 0) continue;

                    double similarity = coCount / Math.sqrt((double) uA * uB);
                    if (similarity <= 0) continue;
                    candidates.add(new SimilarCandidate(similarPostId, similarity));
                }

                candidates.sort(Comparator.comparingDouble(SimilarCandidate::score).reversed());
                int rank = 1;
                for (int i = 0; i < Math.min(candidates.size(), TOP_K); i++) {
                    SimilarCandidate sc = candidates.get(i);
                    PostSimilarity ps = new PostSimilarity();
                    ps.setPostId(postId);
                    ps.setSimilarPostId(sc.postId());
                    ps.setSimilarity(Math.round(sc.score() * 10000.0) / 10000.0);
                    ps.setAlgorithm("item_cf");
                    ps.setRank(rank++);
                    allSimilarities.add(ps);
                }
            }

            if (allSimilarities.isEmpty()) {
                log.warn("ItemCF 无计算结果");
                return;
            }

            postSimilarityMapper.deleteByAlgorithm("item_cf");
            postSimilarityService.saveBatch(allSimilarities, 500);

            long elapsed = System.currentTimeMillis() - start;
            log.info("ItemCF 计算完成，生成 {} 条相似记录，耗时 {} ms", allSimilarities.size(), elapsed);

        } catch (Exception e) {
            log.error("ItemCF 计算失败: {}", e.getMessage(), e);
        }
    }

    private Map<Long, Set<Long>> loadUserPosts() {
        LocalDateTime since = LocalDate.now().minusDays(90).atStartOfDay();
        List<UserBehavior> behaviors = userBehaviorService.lambdaQuery()
                .ge(UserBehavior::getCreateTime, since)
                .isNotNull(UserBehavior::getPostId)
                .and(w -> w.in(UserBehavior::getActionType, "like", "comment")
                        .or(ow -> ow.eq(UserBehavior::getActionType, "view")
                                     .gt(UserBehavior::getDuration, 30)))
                .list();

        Map<Long, Set<Long>> userPosts = new HashMap<>();
        for (UserBehavior ub : behaviors) {
            if (ub.getUserId() == null || ub.getPostId() == null) continue;
            userPosts.computeIfAbsent(ub.getUserId(), k -> new HashSet<>()).add(ub.getPostId());
        }
        return userPosts;
    }

    private record SimilarCandidate(Long postId, double score) {}
}
