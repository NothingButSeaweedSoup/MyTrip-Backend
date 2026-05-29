package com.backend.service.impl;

import com.backend.common.BusinessException;
import com.backend.dto.TagCreateRequest;
import com.backend.dto.TagEditRequest;
import com.backend.dto.TagVO;
import com.backend.entity.Tag;
import com.backend.mapper.TagMapper;
import com.backend.service.TagService;
import com.backend.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Lazy
    @Autowired
    private UserService userService;

    @Override
    public List<Tag> getHotTags(int limit) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getUseCount)
               .last("LIMIT " + Math.min(limit, 100));
        return list(wrapper);
    }

    @Override
    public List<Tag> searchByName(String keyword) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Tag::getName, keyword)
               .orderByDesc(Tag::getUseCount)
               .last("LIMIT 20");
        return list(wrapper);
    }

    @Override
    @Transactional
    public Integer createTag(Long adminUserId, TagCreateRequest request) {
        userService.checkAdminRole(adminUserId);
        if (lambdaQuery().eq(Tag::getName, request.getName()).exists()) {
            throw new BusinessException("标签名已存在");
        }
        Tag tag = new Tag();
        tag.setName(request.getName());
        tag.setUseCount(0);
        save(tag);
        return tag.getTagId();
    }

    @Override
    @Transactional
    public void updateTag(Long adminUserId, Integer tagId, TagEditRequest request) {
        userService.checkAdminRole(adminUserId);
        Tag tag = getById(tagId);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        if (StringUtils.hasText(request.getName())) {
            if (lambdaQuery().eq(Tag::getName, request.getName()).ne(Tag::getTagId, tagId).exists()) {
                throw new BusinessException("标签名已存在");
            }
            tag.setName(request.getName());
        }
        updateById(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long adminUserId, Integer tagId) {
        userService.checkAdminRole(adminUserId);
        Tag tag = getById(tagId);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        removeById(tagId);
    }

    @Override
    public IPage<TagVO> listTagsForAdmin(Long adminUserId, String keyword, int page, int pageSize) {
        userService.checkAdminRole(adminUserId);
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Tag::getName, keyword);
        }
        wrapper.orderByDesc(Tag::getUseCount);
        IPage<Tag> tagPage = page(new Page<>(page, pageSize), wrapper);
        return tagPage.convert(this::toTagVO);
    }

    private TagVO toTagVO(Tag tag) {
        return TagVO.builder()
                .tagId(tag.getTagId())
                .name(tag.getName())
                .useCount(tag.getUseCount())
                .createTime(tag.getCreateTime())
                .build();
    }
}
