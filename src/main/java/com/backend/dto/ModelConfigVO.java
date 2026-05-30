package com.backend.dto;

public class ModelConfigVO {

    private ModelItem chat;
    private ModelItem review;
    private VisionModelItem vision;
    private EmbeddingModelItem embedding;

    public static class ModelItem {
        private String apiKey;
        private String baseUrl;
        private String modelName;
        private double temperature;
        private int maxTokens;
        private int timeoutSeconds;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class VisionModelItem extends ModelItem {
        private int maxImages;

        public int getMaxImages() { return maxImages; }
        public void setMaxImages(int maxImages) { this.maxImages = maxImages; }
    }

    public static class EmbeddingModelItem {
        private String apiKey;
        private String baseUrl;
        private String modelName;
        private int dimensions;
        private int batchSize;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int dimensions) { this.dimensions = dimensions; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }

    public ModelItem getChat() { return chat; }
    public void setChat(ModelItem chat) { this.chat = chat; }
    public ModelItem getReview() { return review; }
    public void setReview(ModelItem review) { this.review = review; }
    public VisionModelItem getVision() { return vision; }
    public void setVision(VisionModelItem vision) { this.vision = vision; }
    public EmbeddingModelItem getEmbedding() { return embedding; }
    public void setEmbedding(EmbeddingModelItem embedding) { this.embedding = embedding; }
}
