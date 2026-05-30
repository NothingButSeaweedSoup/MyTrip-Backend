package com.backend.dto;

import jakarta.validation.constraints.NotNull;

public class ModelConfigUpdateRequest {

    @NotNull
    private ModelConfigVO.ModelItem chat;

    @NotNull
    private ModelConfigVO.ModelItem review;

    private ModelConfigVO.VisionModelItem vision;

    @NotNull
    private ModelConfigVO.EmbeddingModelItem embedding;

    public ModelConfigVO.ModelItem getChat() { return chat; }
    public void setChat(ModelConfigVO.ModelItem chat) { this.chat = chat; }
    public ModelConfigVO.ModelItem getReview() { return review; }
    public void setReview(ModelConfigVO.ModelItem review) { this.review = review; }
    public ModelConfigVO.VisionModelItem getVision() { return vision; }
    public void setVision(ModelConfigVO.VisionModelItem vision) { this.vision = vision; }
    public ModelConfigVO.EmbeddingModelItem getEmbedding() { return embedding; }
    public void setEmbedding(ModelConfigVO.EmbeddingModelItem embedding) { this.embedding = embedding; }
}
