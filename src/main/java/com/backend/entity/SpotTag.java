package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 景点-标签关联表
 * @TableName spot_tag
 */
@TableName(value ="spot_tag")
@Data
public class SpotTag {
    /**
     * 景点ID
     */
    @TableId
    private Long spotId;

    /**
     * 标签ID
     */
    @TableId
    private Integer tagId;

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
        SpotTag other = (SpotTag) that;
        return (this.getSpotId() == null ? other.getSpotId() == null : this.getSpotId().equals(other.getSpotId()))
            && (this.getTagId() == null ? other.getTagId() == null : this.getTagId().equals(other.getTagId()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSpotId() == null) ? 0 : getSpotId().hashCode());
        result = prime * result + ((getTagId() == null) ? 0 : getTagId().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", spotId=").append(spotId);
        sb.append(", tagId=").append(tagId);
        sb.append("]");
        return sb.toString();
    }
}