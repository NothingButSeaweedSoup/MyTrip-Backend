package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.*;
import com.backend.service.PostService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/post")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PostCreateRequest request,
                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Long postId = postService.createPost(userId, request);
        return Result.success(postId);
    }

    @GetMapping("/list")
    public Result<IPage<PostVO>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int pageSize,
                                      @RequestParam(defaultValue = "time") String sortBy,
                                      @RequestParam(defaultValue = "desc") String order) {
        return Result.success(postService.listPosts(page, pageSize, sortBy, order));
    }

    @GetMapping("/{postId}")
    public Result<PostVO> detail(@PathVariable Long postId,
                                Authentication auth) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return Result.success(postService.getPostDetail(postId, userId));
    }

    @PutMapping("/{postId}")
    public Result<Void> edit(@PathVariable Long postId,
                             @Valid @RequestBody PostEditRequest request,
                             Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        postService.editPost(userId, postId, request);
        return Result.success();
    }

    @DeleteMapping("/{postId}")
    public Result<Void> delete(@PathVariable Long postId,
                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        postService.deletePost(userId, postId);
        return Result.success();
    }

    @PostMapping("/{postId}/like")
    public Result<Long> like(@PathVariable Long postId,
                             @Valid @RequestBody ActionRequest request,
                             Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        long likes = postService.toggleLike(userId, postId, request.getAction());
        return Result.success(likes);
    }

    @GetMapping("/my-posts")
    public Result<IPage<PostVO>> myPosts(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.success(postService.listMyPosts(userId, page, pageSize));
    }

    @GetMapping("/by-ids")
    public Result<List<PostVO>> byIds(@RequestParam List<Long> ids,
                                       Authentication auth) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return Result.success(postService.listPostsByIds(ids, userId));
    }
}
