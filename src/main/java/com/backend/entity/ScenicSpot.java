package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 景点表
 * @TableName scenic_spot
 */
@TableName(value ="scenic_spot")
@Data
public class ScenicSpot {
    /**
     * 景点ID
     */
    @TableId(type = IdType.AUTO)
    private Long spotId;

    /**
     * 景点名
     */
    private String name;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 地址
     */
    private String address;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 简介
     */
    private String description;

    /**
     * 标签列表
     */
    private Object tags;

    /**
     * 综合评分 (0.0~5.0)
     */
    private BigDecimal rating;

    /**
     * 建议游玩时长(分钟)
     */
    private Integer visitDuration;

    /**
     * 开放时间
     */
    private String openTime;

    /**
     * 电话
     */
    private String phone;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 状态: 0-正常, 1-下架
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

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
        ScenicSpot other = (ScenicSpot) that;
        return (this.getSpotId() == null ? other.getSpotId() == null : this.getSpotId().equals(other.getSpotId()))
            && (this.getName() == null ? other.getName() == null : this.getName().equals(other.getName()))
            && (this.getCity() == null ? other.getCity() == null : this.getCity().equals(other.getCity()))
            && (this.getAddress() == null ? other.getAddress() == null : this.getAddress().equals(other.getAddress()))
            && (this.getLatitude() == null ? other.getLatitude() == null : this.getLatitude().equals(other.getLatitude()))
            && (this.getLongitude() == null ? other.getLongitude() == null : this.getLongitude().equals(other.getLongitude()))
            && (this.getDescription() == null ? other.getDescription() == null : this.getDescription().equals(other.getDescription()))
            && (this.getTags() == null ? other.getTags() == null : this.getTags().equals(other.getTags()))
            && (this.getRating() == null ? other.getRating() == null : this.getRating().equals(other.getRating()))
            && (this.getVisitDuration() == null ? other.getVisitDuration() == null : this.getVisitDuration().equals(other.getVisitDuration()))
            && (this.getOpenTime() == null ? other.getOpenTime() == null : this.getOpenTime().equals(other.getOpenTime()))
            && (this.getPhone() == null ? other.getPhone() == null : this.getPhone().equals(other.getPhone()))
            && (this.getCoverImage() == null ? other.getCoverImage() == null : this.getCoverImage().equals(other.getCoverImage()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getSpotId() == null) ? 0 : getSpotId().hashCode());
        result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
        result = prime * result + ((getCity() == null) ? 0 : getCity().hashCode());
        result = prime * result + ((getAddress() == null) ? 0 : getAddress().hashCode());
        result = prime * result + ((getLatitude() == null) ? 0 : getLatitude().hashCode());
        result = prime * result + ((getLongitude() == null) ? 0 : getLongitude().hashCode());
        result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
        result = prime * result + ((getTags() == null) ? 0 : getTags().hashCode());
        result = prime * result + ((getRating() == null) ? 0 : getRating().hashCode());
        result = prime * result + ((getVisitDuration() == null) ? 0 : getVisitDuration().hashCode());
        result = prime * result + ((getOpenTime() == null) ? 0 : getOpenTime().hashCode());
        result = prime * result + ((getPhone() == null) ? 0 : getPhone().hashCode());
        result = prime * result + ((getCoverImage() == null) ? 0 : getCoverImage().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", spotId=").append(spotId);
        sb.append(", name=").append(name);
        sb.append(", city=").append(city);
        sb.append(", address=").append(address);
        sb.append(", latitude=").append(latitude);
        sb.append(", longitude=").append(longitude);
        sb.append(", description=").append(description);
        sb.append(", tags=").append(tags);
        sb.append(", rating=").append(rating);
        sb.append(", visitDuration=").append(visitDuration);
        sb.append(", openTime=").append(openTime);
        sb.append(", phone=").append(phone);
        sb.append(", coverImage=").append(coverImage);
        sb.append(", status=").append(status);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append("]");
        return sb.toString();
    }
}