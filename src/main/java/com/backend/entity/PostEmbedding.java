package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Arrays;
import java.util.Date;
import lombok.Data;

/**
 * 帖子向量表
 * @TableName post_embedding
 */
@TableName(value ="post_embedding")
@Data
public class PostEmbedding {
    /**
     * 帖子ID (一对一关联post)
     */
    @TableId
    private Long postId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 向量数据 (768维float二进制)
     */
    private byte[] embedding;

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
        PostEmbedding other = (PostEmbedding) that;
        return (this.getPostId() == null ? other.getPostId() == null : this.getPostId().equals(other.getPostId()))
            && (this.getModelName() == null ? other.getModelName() == null : this.getModelName().equals(other.getModelName()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (Arrays.equals(this.getEmbedding(), other.getEmbedding()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getPostId() == null) ? 0 : getPostId().hashCode());
        result = prime * result + ((getModelName() == null) ? 0 : getModelName().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + (Arrays.hashCode(getEmbedding()));
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", postId=").append(postId);
        sb.append(", modelName=").append(modelName);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", embedding=").append(embedding);
        sb.append("]");
        return sb.toString();
    }
}