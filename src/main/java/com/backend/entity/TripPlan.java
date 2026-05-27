package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * AI行程计划表
 * @TableName trip_plan
 */
@TableName(value ="trip_plan")
@Data
public class TripPlan {
    /**
     * 计划ID
     */
    @TableId(type = IdType.AUTO)
    private Long planId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 计划名称
     */
    private String title;

    /**
     * 天数
     */
    private Integer days;

    /**
     * 偏好标签
     */
    private String preferences;

    /**
     * 预算: low/middle/high
     */
    private String budget;

    /**
     * 起始日期
     */
    private Date startDate;

    /**
     * 高德折线编码
     */
    private String mapPolyline;

    /**
     * 每日天气摘要
     */
    private String weatherInfo;

    /**
     * 创建时间
     */
    private Date createTime;

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
        TripPlan other = (TripPlan) that;
        return (this.getPlanId() == null ? other.getPlanId() == null : this.getPlanId().equals(other.getPlanId()))
            && (this.getUserId() == null ? other.getUserId() == null : this.getUserId().equals(other.getUserId()))
            && (this.getTitle() == null ? other.getTitle() == null : this.getTitle().equals(other.getTitle()))
            && (this.getDays() == null ? other.getDays() == null : this.getDays().equals(other.getDays()))
            && (this.getPreferences() == null ? other.getPreferences() == null : this.getPreferences().equals(other.getPreferences()))
            && (this.getBudget() == null ? other.getBudget() == null : this.getBudget().equals(other.getBudget()))
            && (this.getStartDate() == null ? other.getStartDate() == null : this.getStartDate().equals(other.getStartDate()))
            && (this.getMapPolyline() == null ? other.getMapPolyline() == null : this.getMapPolyline().equals(other.getMapPolyline()))
            && (this.getWeatherInfo() == null ? other.getWeatherInfo() == null : this.getWeatherInfo().equals(other.getWeatherInfo()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getPlanId() == null) ? 0 : getPlanId().hashCode());
        result = prime * result + ((getUserId() == null) ? 0 : getUserId().hashCode());
        result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
        result = prime * result + ((getDays() == null) ? 0 : getDays().hashCode());
        result = prime * result + ((getPreferences() == null) ? 0 : getPreferences().hashCode());
        result = prime * result + ((getBudget() == null) ? 0 : getBudget().hashCode());
        result = prime * result + ((getStartDate() == null) ? 0 : getStartDate().hashCode());
        result = prime * result + ((getMapPolyline() == null) ? 0 : getMapPolyline().hashCode());
        result = prime * result + ((getWeatherInfo() == null) ? 0 : getWeatherInfo().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", planId=").append(planId);
        sb.append(", userId=").append(userId);
        sb.append(", title=").append(title);
        sb.append(", days=").append(days);
        sb.append(", preferences=").append(preferences);
        sb.append(", budget=").append(budget);
        sb.append(", startDate=").append(startDate);
        sb.append(", mapPolyline=").append(mapPolyline);
        sb.append(", weatherInfo=").append(weatherInfo);
        sb.append(", createTime=").append(createTime);
        sb.append("]");
        return sb.toString();
    }
}
