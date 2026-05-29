package com.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ScenicSpotEditRequest {
    @Size(max = 100, message = "景点名最长100字")
    private String name;

    @Size(max = 50, message = "城市名最长50字")
    private String city;

    @Size(max = 200, message = "地址最长200字")
    private String address;

    @DecimalMin(value = "-90", message = "纬度范围 -90~90")
    @DecimalMax(value = "90", message = "纬度范围 -90~90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180", message = "经度范围 -180~180")
    @DecimalMax(value = "180", message = "经度范围 -180~180")
    private BigDecimal longitude;

    @Size(max = 2000, message = "简介最长2000字")
    private String description;

    private List<Integer> tagIds;

    @DecimalMin(value = "0", message = "评分最低0")
    @DecimalMax(value = "5", message = "评分最高5")
    private BigDecimal rating;

    @Min(value = 0, message = "游玩时长不能为负")
    private Integer visitDuration;

    @Size(max = 100, message = "开放时间最长100字")
    private String openTime;

    @Size(max = 20, message = "电话最长20字")
    private String phone;

    @Size(max = 500, message = "封面图URL最长500字")
    private String coverImage;
}
