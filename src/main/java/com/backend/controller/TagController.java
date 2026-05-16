package com.backend.controller;

import com.backend.common.Result;
import com.backend.entity.Tag;
import com.backend.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping("/hot")
    public Result<List<Tag>> hot(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(tagService.getHotTags(limit));
    }
}
