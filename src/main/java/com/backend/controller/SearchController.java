package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.PostVO;
import com.backend.service.SearchService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    public Result<IPage<PostVO>> search(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(searchService.search(keyword, page, pageSize));
    }

    @GetMapping("/suggest")
    public Result<List<String>> suggest(@RequestParam String keyword) {
        return Result.success(searchService.suggest(keyword));
    }
}
