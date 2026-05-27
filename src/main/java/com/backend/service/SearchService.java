package com.backend.service;

import com.backend.dto.PostVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface SearchService {

    /**
     * 混合搜索：关键词 + 语义，按权重融合排序。
     * mode=hybrid 时的默认入口。
     */
    IPage<PostVO> hybridSearch(String keyword, int page, int pageSize, Long userId, double semanticWeight);

    /** 纯语义搜索 */
    IPage<PostVO> semanticSearch(String keyword, int page, int pageSize, Long userId);

    /** 纯关键词搜索 */
    IPage<PostVO> keywordSearch(String keyword, int page, int pageSize, Long userId);

    /** 搜索建议（返回匹配标签名） */
    List<String> suggest(String keyword);
}
