package com.backend.service;

import com.backend.dto.PostVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface RecommendService {

    /** 推荐流（随机抽取） */
    IPage<PostVO> getFeed(Long userId, int page, int pageSize);

    /** 上报用户行为 */
    void reportBehavior(Long userId, Long postId, String actionType, Integer duration);
}
