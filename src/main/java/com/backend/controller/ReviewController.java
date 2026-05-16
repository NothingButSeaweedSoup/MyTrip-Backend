package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.AuditRequest;
import com.backend.entity.Comment;
import com.backend.entity.Post;
import com.backend.service.CommentService;
import com.backend.service.PostAuditRecordService;
import com.backend.service.PostService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/review")
public class ReviewController {

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostAuditRecordService auditRecordService;

    /** 待审核帖子列表 */
    @GetMapping("/post/list")
    public Result<IPage<Post>> listPosts(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int pageSize) {
        LambdaQueryWrapper<com.backend.entity.Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 0)
               .orderByDesc(Post::getCreateTime);
        IPage<com.backend.entity.Post> postPage = postService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize), wrapper);
        // 简单返回，不组装完整VO
        return Result.success(postPage);
    }

    /** 审核帖子 */
    @PutMapping("/post/{postId}")
    public Result<Void> auditPost(@PathVariable Long postId,
                                  @Valid @RequestBody AuditRequest request,
                                  Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        auditRecordService.auditPost(userId, postId, request.getAction(), request.getRemark());
        return Result.success();
    }

    /** 待审核评论列表 */
    @GetMapping("/comment/list")
    public Result<IPage<Comment>> listComments(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int pageSize) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getStatus, 1)
               .orderByDesc(Comment::getCreateTime);
        return Result.success(commentService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize), wrapper));
    }

    /** 审核评论 */
    @PutMapping("/comment/{commentId}")
    public Result<Void> auditComment(@PathVariable Long commentId,
                                     @Valid @RequestBody AuditRequest request,
                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        auditRecordService.auditComment(userId, commentId, request.getAction(), request.getRemark());
        return Result.success();
    }
}
