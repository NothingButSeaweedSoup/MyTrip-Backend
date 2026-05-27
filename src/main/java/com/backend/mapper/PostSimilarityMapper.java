package com.backend.mapper;

import com.backend.entity.PostSimilarity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
* @author Administrator
* @description 针对表【post_similarity(帖子相似表)】的数据库操作Mapper
* @createDate 2026-05-16 00:16:36
* @Entity com.backend.entity.PostSimilarity
*/
import org.apache.ibatis.annotations.Delete;

public interface PostSimilarityMapper extends BaseMapper<PostSimilarity> {

    /** 根据算法标识删除旧数据 */
    @Delete("DELETE FROM post_similarity WHERE algorithm = #{algorithm}")
    int deleteByAlgorithm(String algorithm);
}




