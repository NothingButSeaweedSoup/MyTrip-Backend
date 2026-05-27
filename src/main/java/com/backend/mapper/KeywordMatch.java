package com.backend.mapper;

/**
 * 关键词搜索匹配结果
 */
public class KeywordMatch {

    private Long postId;
    private Double score;

    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
