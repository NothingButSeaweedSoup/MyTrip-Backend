package com.backend.service;

import java.util.List;

public interface EmbeddingService {

    /** 将文本转为向量 (768维float数组) */
    float[] embed(String text);

    /** 批量将文本转为向量 */
    List<float[]> embedBatch(List<String> texts);

    /** 计算两个向量的余弦相似度 */
    double cosineSimilarity(float[] a, float[] b);

    /** 将byte[]反序列化为float[] (MySQL BLOB -> float数组) */
    float[] deserialize(byte[] embeddingBlob);

    /** 将float[]序列化为byte[] (float数组 -> MySQL BLOB) */
    byte[] serialize(float[] vector);
}
