package com.backend.service.impl;

import com.backend.entity.Post;
import com.backend.entity.PostImage;
import com.backend.entity.User;
import com.backend.service.AiReviewService;
import com.backend.service.PostAuditRecordService;
import com.backend.service.PostImageService;
import com.backend.service.PostService;
import com.backend.service.UserService;
import com.backend.util.ACAutomaton;
import com.backend.util.ImageUrlUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 三级自动审核流水线：
 * 0. 规则检查（新用户转人工）
 * 1. 敏感词过滤（AC 自动机）
 * 2. AI 审核（LangChain4j ChatModel）
 * 3. 以上均无法决定 → 转人工
 *
 * 职责：编排审核步骤，不直接操作持久层。
 */
@Component
public class ReviewPipeline {

    private static final Logger log = LoggerFactory.getLogger(ReviewPipeline.class);

    @Autowired
    private ACAutomaton acAutomaton;

    @Autowired
    private AiReviewService aiReviewService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostAuditRecordService auditRecordService;

    @Autowired
    private PostImageService postImageService;

    @Autowired
    private ImageUrlUtil imageUrlUtil;

    @Async("reviewExecutor")
    public void autoReview(Long postId) {
        try {
            doAutoReview(postId);
        } catch (Exception e) {
            log.error("自动审核异常 postId={}: {}", postId, e.getMessage(), e);
        }
    }

    private void doAutoReview(Long postId) {
        Post post = postService.getById(postId);
        if (post == null || post.getStatus() != 0) return;

        // 0. 规则检查
        User author = userService.getById(post.getAuthorId());
        if (author != null && isNewUser(author)) {
            auditRecordService.autoAuditPost(postId, 3, "新注册用户（注册不足 24 小时），转人工审核");
            log.info("审核转人工 postId={}, reason=新注册用户", postId);
            return;
        }

        // 1. 敏感词过滤
        String text = post.getTitle() + "\n" + post.getContent();
        String hitWord = acAutomaton.match(text);
        if (hitWord != null) {
            auditRecordService.autoAuditPost(postId, 2, "包含敏感词: " + hitWord);
            log.info("审核驳回 postId={}, reason=敏感词: {}", postId, hitWord);
            return;
        }

        // 2. AI 审核（含图片）
        LambdaQueryWrapper<PostImage> imgWrapper = new LambdaQueryWrapper<>();
        imgWrapper.eq(PostImage::getPostId, postId);
        List<String> imageUrls = postImageService.list(imgWrapper).stream()
                .map(PostImage::getUrl)
                .map(url -> "http://backend:8180" + (url.startsWith("/") ? url : "/" + url))
                .collect(Collectors.toList());

        AiReviewService.AiReviewResult aiResult;
        if (!imageUrls.isEmpty()) {
            aiResult = aiReviewService.reviewWithImages(post.getTitle(), post.getContent(), imageUrls);
        } else {
            aiResult = aiReviewService.review(post.getTitle(), post.getContent());
        }

        int action;
        if (aiResult.isApproved()) {
            action = 1;
        } else if (aiResult.isRejected()) {
            action = 2;
        } else {
            action = 3;
        }

        auditRecordService.autoAuditPost(postId, action, aiResult.reason());
        log.info("审核完成 postId={}, action={}, reason={}", postId, action, aiResult.reason());
    }

    private boolean isNewUser(User user) {
        if (user.getCreateTime() == null) return false;
        return ChronoUnit.HOURS.between(
                user.getCreateTime().toInstant(), Instant.now()) < 24;
    }
}
