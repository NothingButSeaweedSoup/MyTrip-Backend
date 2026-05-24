package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.ActionRequest;
import com.backend.dto.PostVO;
import com.backend.service.FavoriteService;
import com.backend.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private PostService postService;

    @PostMapping("/{postId}")
    public Result<Long> toggle(@PathVariable Long postId,
                               @Valid @RequestBody ActionRequest request,
                               Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        long count = favoriteService.toggleFavorite(userId, postId, request.getAction());
        return Result.success(count);
    }

    @GetMapping("/list")
    public Result<List<PostVO>> list(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int pageSize,
                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        List<Long> ids = favoriteService.getUserFavoriteIds(userId, page, pageSize);
        return Result.success(postService.listPostsByIds(ids, userId));
    }

    @GetMapping("/check/{postId}")
    public Result<Boolean> check(@PathVariable Long postId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.success(favoriteService.isFavorited(userId, postId));
    }
}
