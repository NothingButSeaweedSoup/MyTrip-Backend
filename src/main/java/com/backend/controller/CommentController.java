package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.*;
import com.backend.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CommentCreateRequest request,
                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Long commentId = commentService.createComment(userId, request);
        return Result.success(commentId);
    }

    @GetMapping("/list/{postId}")
    public Result<List<CommentVO>> list(@PathVariable Long postId) {
        return Result.success(commentService.getCommentTree(postId));
    }

    @DeleteMapping("/{commentId}")
    public Result<Void> delete(@PathVariable Long commentId,
                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        commentService.deleteComment(userId, commentId);
        return Result.success();
    }

    @PostMapping("/{commentId}/like")
    public Result<Long> like(@PathVariable Long commentId,
                             @Valid @RequestBody ActionRequest request,
                             Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        long likes = commentService.toggleLike(userId, commentId, request.getAction());
        return Result.success(likes);
    }
}
