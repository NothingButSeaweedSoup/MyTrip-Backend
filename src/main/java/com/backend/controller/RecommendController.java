package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.PostVO;
import com.backend.service.RecommendService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("/feed")
    public Result<IPage<PostVO>> feed(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int pageSize,
                                      Authentication auth) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return Result.success(recommendService.getFeed(userId, page, pageSize));
    }

    @PostMapping("/behavior")
    public Result<Void> behavior(@RequestBody Map<String, Object> body,
                                 Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Long postId = body.get("postId") != null ? Long.valueOf(body.get("postId").toString()) : null;
        String actionType = (String) body.get("actionType");
        Integer duration = body.get("duration") != null ? Integer.valueOf(body.get("duration").toString()) : null;

        recommendService.reportBehavior(userId, postId, actionType, duration);
        return Result.success();
    }
}
