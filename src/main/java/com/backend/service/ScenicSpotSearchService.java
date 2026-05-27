package com.backend.service;

import com.backend.dto.ScenicSpotVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

public interface ScenicSpotSearchService {

    /** 混合搜索景点（关键词 + 语义） */
    IPage<ScenicSpotVO> search(String keyword, int page, int pageSize, double semanticWeight);

    /** 纯语义搜索景点 */
    IPage<ScenicSpotVO> semanticSearch(String keyword, int page, int pageSize);

    /** 纯关键词搜索景点 */
    IPage<ScenicSpotVO> keywordSearch(String keyword, int page, int pageSize);
}
