package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.AuditRequest;
import com.backend.dto.PostVO;
import com.backend.entity.Comment;
import com.backend.entity.Post;
import com.backend.service.CommentService;
import com.backend.service.PostAuditRecordService;
import com.backend.service.PostService;
import com.backend.util.ReviewLockUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostAuditRecordService auditRecordService;

    @Autowired
    private ReviewLockUtil reviewLockUtil;

    @GetMapping("/post/list")
    public Result<IPage<Post>> listPosts(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(postService.listPendingPosts(page, pageSize));
    }

    @GetMapping("/post/next")
    public Result<PostVO> nextPost(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Set<Long> lockedIds = reviewLockUtil.getLockedPostIds();
        Post post = postService.getNextPendingPost(lockedIds);
        if (post == null) {
            return Result.success(null);
        }
        reviewLockUtil.tryAcquire(post.getPostId(), userId);
        return Result.success(postService.toPostVO(post, userId));
    }

    @PostMapping("/post/{postId}/lock")
    public Result<Boolean> acquireLock(@PathVariable Long postId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean acquired = reviewLockUtil.tryAcquire(postId, userId);
        return Result.success(acquired);
    }

    @PutMapping("/post/{postId}/lock")
    public Result<Boolean> renewLock(@PathVariable Long postId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        boolean renewed = reviewLockUtil.renew(postId, userId);
        return Result.success(renewed);
    }

    @DeleteMapping("/post/{postId}/lock")
    public Result<Void> releaseLock(@PathVariable Long postId) {
        reviewLockUtil.release(postId);
        return Result.success();
    }

    @PutMapping("/post/{postId}")
    public Result<Void> auditPost(@PathVariable Long postId,
                                  @Valid @RequestBody AuditRequest request,
                                  Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        auditRecordService.auditPost(userId, postId, request.getAction(), request.getRemark());
        reviewLockUtil.release(postId);
        return Result.success();
    }

    @GetMapping("/comment/list")
    public Result<IPage<Comment>> listComments(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(commentService.listPendingComments(page, pageSize));
    }

    @PutMapping("/comment/{commentId}")
    public Result<Void> auditComment(@PathVariable Long commentId,
                                     @Valid @RequestBody AuditRequest request,
                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        auditRecordService.auditComment(userId, commentId, request.getAction(), request.getRemark());
        return Result.success();
    }
}
