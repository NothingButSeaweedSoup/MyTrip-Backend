package com.backend.service;

import com.backend.dto.PostVO;
import com.backend.dto.RecommendConfigVO;
import com.backend.dto.RecommendConfigUpdateRequest;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface RecommendService {

    /** 推荐流（随机抽取） */
    IPage<PostVO> getFeed(Long userId, int page, int pageSize);

    /** 上报用户行为 */
    void reportBehavior(Long userId, Long postId, String actionType, Integer duration);

    /** 获取推荐配置 */
    RecommendConfigVO getConfig();

    /** 更新推荐配置 */
    void updateConfig(Long adminUserId, RecommendConfigUpdateRequest request);
}
