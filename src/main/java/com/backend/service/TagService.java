package com.backend.service;

import com.backend.entity.Tag;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TagService extends IService<Tag> {

    /** 热门标签（按使用次数降序） */
    List<Tag> getHotTags(int limit);

    /** 按名称搜索标签 */
    List<Tag> searchByName(String keyword);
}
