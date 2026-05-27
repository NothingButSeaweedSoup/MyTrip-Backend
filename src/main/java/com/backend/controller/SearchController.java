package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.PostVO;
import com.backend.dto.ScenicSpotVO;
import com.backend.service.ScenicSpotSearchService;
import com.backend.service.SearchService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private ScenicSpotSearchService scenicSpotSearchService;

    @Value("${search.hybrid.default-semantic-weight:0.5}")
    private double defaultSemanticWeight;

    /**
     * 帖子混合搜索（默认）。
     *
     * @param keyword        搜索关键词
     * @param page           页码 (从1开始)
     * @param pageSize       每页条数
     * @param mode           搜索模式: hybrid(默认) / semantic / keyword
     * @param semanticWeight 语义权重 (0~1, 默认0.5)，仅 hybrid 模式有效
     */
    @GetMapping
    public Result<IPage<PostVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "hybrid") String mode,
            @RequestParam(required = false) Double semanticWeight,
            Authentication auth) {

        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        double sw = semanticWeight != null ? semanticWeight : defaultSemanticWeight;

        IPage<PostVO> result = switch (mode) {
            case "semantic" -> searchService.semanticSearch(keyword, page, pageSize, userId);
            case "keyword" -> searchService.keywordSearch(keyword, page, pageSize, userId);
            default -> searchService.hybridSearch(keyword, page, pageSize, userId, sw);
        };

        return Result.success(result);
    }

    /** 景点搜索 */
    @GetMapping("/scenic-spots")
    public Result<IPage<ScenicSpotVO>> searchScenicSpots(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Double semanticWeight) {
        double sw = semanticWeight != null ? semanticWeight : defaultSemanticWeight;
        return Result.success(scenicSpotSearchService.search(keyword, page, pageSize, sw));
    }

    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam String keyword) {
        return Result.success(searchService.suggest(keyword));
    }
}
