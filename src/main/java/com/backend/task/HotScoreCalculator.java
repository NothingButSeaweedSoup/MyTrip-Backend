package com.backend.task;

import com.backend.mapper.PostMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HotScoreCalculator {

    private static final Logger log = LoggerFactory.getLogger(HotScoreCalculator.class);

    @Autowired
    private PostMapper postMapper;

    /**
     * 热度分计算公式：
     *   hotScore = log10(views + 1) × 0.3
     *            + log10(likes + 1) × 0.4
     *            + log10(comment_count + 1) × 0.2
     *            + (1 / (days_since_publish + 3)) × 0.1
     *
     * 每 30 分钟执行一次，仅更新 score_updated_at 过期的帖子。
     */
    @Scheduled(fixedRate = 1_800_000)
    public void calculateHotScores() {
        try {
            int updated = postMapper.batchUpdateHotScore();
            if (updated > 0) {
                log.info("热度分更新完成，本次更新 {} 条帖子", updated);
            }
        } catch (Exception e) {
            log.warn("热度分计算失败: {}", e.getMessage());
        }
    }
}
