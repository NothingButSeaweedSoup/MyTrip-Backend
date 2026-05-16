package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Arrays;
import java.util.Date;
import lombok.Data;

/**
 * 景点向量表
 * @TableName scenic_spot_embedding
 */
@TableName(value ="scenic_spot_embedding")
@Data
public class ScenicSpotEmbedding {
    /**
     * 景点ID (一对一关联scenic_spot)
     */
    @TableId
    private Long spotId;

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
        ScenicSpotEmbedding other = (ScenicSpotEmbedding) that;
        return (this.getSpotId() == null ? other.getSpotId() == null : this.getSpotId().equals(other.getSpotId()))
            && (this.getModelName() == null ? other.getModelName() == null : this.getModelName().equals(other.getModelName()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (Arrays.equals(this.getEmbedding(), other.getEmbedding()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSpotId() == null) ? 0 : getSpotId().hashCode());
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
        sb.append(", spotId=").append(spotId);
        sb.append(", modelName=").append(modelName);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", embedding=").append(embedding);
        sb.append("]");
        return sb.toString();
    }
}