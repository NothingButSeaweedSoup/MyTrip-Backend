package com.backend.service.impl;

import com.backend.service.EmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceImpl.class);

    @Autowired
    private EmbeddingModel siliconFlowEmbeddingModel;

    @Override
    public float[] embed(String text) {
        Embedding embedding = siliconFlowEmbeddingModel.embed(text).content();
        return embedding.vector();
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<TextSegment> segments = texts.stream()
                .map(TextSegment::from)
                .collect(Collectors.toList());
        List<Embedding> embeddings = siliconFlowEmbeddingModel.embedAll(segments).content();
        List<float[]> result = new ArrayList<>(embeddings.size());
        for (Embedding e : embeddings) {
            result.add(e.vector());
        }
        return result;
    }

    @Override
    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("向量维度不匹配: " + a.length + " vs " + b.length);
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0 : dotProduct / denominator;
    }

    @Override
    public float[] deserialize(byte[] embeddingBlob) {
        if (embeddingBlob == null) return null;
        ByteBuffer buffer = ByteBuffer.wrap(embeddingBlob);
        int dim = embeddingBlob.length / 4;
        float[] vector = new float[dim];
        for (int i = 0; i < dim; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }

    @Override
    public byte[] serialize(float[] vector) {
        if (vector == null) return null;
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4);
        for (float v : vector) {
            buffer.putFloat(v);
        }
        return buffer.array();
    }
}
