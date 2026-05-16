package com.backend.service.impl;

import com.backend.dto.PostVO;
import com.backend.entity.Post;
import com.backend.entity.Tag;
import com.backend.service.PostService;
import com.backend.service.SearchService;
import com.backend.service.TagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private PostService postService;

    @Autowired
    private TagService tagService;

    @Override
    public IPage<PostVO> search(String keyword, int page, int pageSize) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1)
               .and(w -> w.like(Post::getTitle, keyword)
                          .or()
                          .like(Post::getContent, keyword))
               .orderByDesc(Post::getCreateTime);

        Page<Post> postPage = postService.page(new Page<>(page, pageSize), wrapper);
        return postPage.convert(postService::toPostVO);
    }

    @Override
    public List<String> suggest(String keyword) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Tag::getName, keyword)
               .orderByDesc(Tag::getUseCount)
               .last("LIMIT 10");
        return tagService.list(wrapper).stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
    }

}
