package com.backend.config.model;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DynamicEmbeddingModel implements EmbeddingModel {

    private final AtomicReference<EmbeddingModel> delegate;

    public DynamicEmbeddingModel(EmbeddingModel initial) {
        this.delegate = new AtomicReference<>(initial);
    }

    public void swap(EmbeddingModel newModel) {
        this.delegate.set(newModel);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        return delegate.get().embedAll(textSegments);
    }
}
