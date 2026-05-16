package com.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 帖子相似表（复合主键：post_id + similar_post_id）
 * @TableName post_similarity
 */
@TableName(value ="post_similarity")
@Data
public class PostSimilarity {
    /**
     * 源帖子ID
     */
    private Long postId;

    /**
     * 相似帖子ID
     */
    private Long similarPostId;

    /**
     * 相似度得分 (0~1)
     */
    private Double similarity;

    /**
     * 排序序号 (1~N)
     */
    private Integer rank;

    /**
     * 算法标识: item_cf / vector_cosine
     */
    private String algorithm;

    /**
     * 更新时间
     */
    private Date updateTime;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        PostSimilarity other = (PostSimilarity) that;
        return (this.getPostId() == null ? other.getPostId() == null : this.getPostId().equals(other.getPostId()))
            && (this.getSimilarPostId() == null ? other.getSimilarPostId() == null : this.getSimilarPostId().equals(other.getSimilarPostId()))
            && (this.getSimilarity() == null ? other.getSimilarity() == null : this.getSimilarity().equals(other.getSimilarity()))
            && (this.getRank() == null ? other.getRank() == null : this.getRank().equals(other.getRank()))
            && (this.getAlgorithm() == null ? other.getAlgorithm() == null : this.getAlgorithm().equals(other.getAlgorithm()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getPostId() == null) ? 0 : getPostId().hashCode());
        result = prime * result + ((getSimilarPostId() == null) ? 0 : getSimilarPostId().hashCode());
        result = prime * result + ((getSimilarity() == null) ? 0 : getSimilarity().hashCode());
        result = prime * result + ((getRank() == null) ? 0 : getRank().hashCode());
        result = prime * result + ((getAlgorithm() == null) ? 0 : getAlgorithm().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", postId=").append(postId);
        sb.append(", similarPostId=").append(similarPostId);
        sb.append(", similarity=").append(similarity);
        sb.append(", rank=").append(rank);
        sb.append(", algorithm=").append(algorithm);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}