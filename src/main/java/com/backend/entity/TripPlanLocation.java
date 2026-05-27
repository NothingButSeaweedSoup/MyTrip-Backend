package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 行程计划地点表
 * @TableName trip_plan_location
 */
@TableName(value = "trip_plan_location")
@Data
public class TripPlanLocation {
    /**
     * 地点ID
     */
    @TableId(type = IdType.AUTO)
    private Long locationId;

    /**
     * 所属行程计划ID
     */
    private Long planId;

    /**
     * 地点名
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
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 第几天 (1-based, 用于行程分组)
     */
    private Integer dayNumber;

    /**
     * 同一天内的排序
     */
    private Integer sortOrder;

    /**
     * 时间段: 上午/下午/晚上
     */
    private String timeSlot;

    /**
     * 建议游玩时长
     */
    private String duration;

    /**
     * 交通方式说明
     */
    private String transport;

    /**
     * 简短备注
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;
}
