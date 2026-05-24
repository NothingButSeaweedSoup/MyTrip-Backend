package com.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.backend.entity.Tag;
import com.backend.service.TagService;
import com.backend.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

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
}
