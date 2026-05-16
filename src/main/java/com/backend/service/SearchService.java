package com.backend.service;

import com.backend.dto.PostVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface SearchService {

    /** 关键字搜索帖子 */
    IPage<PostVO> search(String keyword, int page, int pageSize);

    /** 搜索建议（返回匹配标签名） */
    List<String> suggest(String keyword);
}
