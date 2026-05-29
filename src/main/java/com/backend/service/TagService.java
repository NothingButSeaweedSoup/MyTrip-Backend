package com.backend.service;

import com.backend.dto.TagCreateRequest;
import com.backend.dto.TagEditRequest;
import com.backend.dto.TagVO;
import com.backend.entity.Tag;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TagService extends IService<Tag> {

    /** 热门标签（按使用次数降序） */
    List<Tag> getHotTags(int limit);

    /** 按名称搜索标签 */
    List<Tag> searchByName(String keyword);

    /** 管理员：新增标签 */
    Integer createTag(Long adminUserId, TagCreateRequest request);

    /** 管理员：编辑标签 */
    void updateTag(Long adminUserId, Integer tagId, TagEditRequest request);

    /** 管理员：删除标签 */
    void deleteTag(Long adminUserId, Integer tagId);

    /** 管理员：标签列表（分页） */
    IPage<TagVO> listTagsForAdmin(Long adminUserId, String keyword, int page, int pageSize);
}
