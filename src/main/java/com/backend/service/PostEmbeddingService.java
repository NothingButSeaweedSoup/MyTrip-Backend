package com.backend.service;

import com.backend.entity.PostEmbedding;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Administrator
* @description 针对表【post_embedding(帖子向量表)】的数据库操作Service
* @createDate 2026-05-16 00:16:36
*/
public interface PostEmbeddingService extends IService<PostEmbedding> {

    /** 保存或更新帖子的向量 */
    void saveEmbedding(Long postId, float[] vector, String modelName);

    /** 获取帖子的向量 (反序列化为float[]) */
    float[] getEmbedding(Long postId);

    /** 查找与目标帖子最相似的topK个帖子 (暴力余弦相似度) */
    List<Long> searchSimilar(Long targetId, int topK);

    /** 查找与查询向量最相似的topK个已审核帖子，返回 postId → 相似度 的映射 */
    java.util.Map<Long, Double> searchByVector(float[] queryVector, int topK);
}
