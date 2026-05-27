package com.backend.service;

import com.backend.entity.ScenicSpotEmbedding;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
* @author Administrator
* @description 针对表【scenic_spot_embedding(景点向量表)】的数据库操作Service
* @createDate 2026-05-16 00:16:36
*/
public interface ScenicSpotEmbeddingService extends IService<ScenicSpotEmbedding> {

    /** 保存或更新景点的向量 */
    void saveEmbedding(Long spotId, float[] vector, String modelName);

    /** 获取景点的向量 */
    float[] getEmbedding(Long spotId);

    /** 查找与查询向量最相似的topK个景点 */
    Map<Long, Double> searchByVector(float[] queryVector, int topK);
}
