package com.backend.service.impl;

import com.backend.dto.PostVO;
import com.backend.entity.Post;
import com.backend.entity.PostTag;
import com.backend.entity.Tag;
import com.backend.service.PostService;
import com.backend.service.PostTagService;
import com.backend.service.SearchService;
import com.backend.service.TagService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private PostService postService;

    @Autowired
    private TagService tagService;

    @Autowired
    private PostTagService postTagService;

    @Override
    public IPage<PostVO> search(String keyword, int page, int pageSize) {
        String kw = "%" + keyword + "%";

        Set<Long> tagPostIds = getPostIdsByTag(keyword);

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1)
               .and(w -> w.like(Post::getTitle, kw)
                          .or()
                          .like(Post::getContent, kw));
        if (!tagPostIds.isEmpty()) {
            wrapper.or().in(Post::getPostId, tagPostIds);
        }
        wrapper.orderByDesc(Post::getCreateTime);

        Page<Post> postPage = postService.page(new Page<>(page, pageSize), wrapper);
        return postPage.convert(postService::toPostVO);
    }

    private Set<Long> getPostIdsByTag(String keyword) {
        List<Tag> tags = tagService.list(
                new LambdaQueryWrapper<Tag>().like(Tag::getName, keyword));
        if (tags.isEmpty()) return Collections.emptySet();
        List<Integer> tagIds = tags.stream().map(Tag::getTagId).toList();
        return postTagService.list(
                new LambdaQueryWrapper<PostTag>().in(PostTag::getTagId, tagIds))
                .stream().map(PostTag::getPostId).collect(Collectors.toSet());
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
