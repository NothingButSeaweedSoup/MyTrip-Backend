package com.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ScenicSpotVO {
    private Long spotId;
    private String name;
    private String city;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String description;
    private List<String> tags;
    private BigDecimal rating;
    private Integer visitDuration;
    private String openTime;
    private String phone;
    private String coverImage;
    private Integer status;
}
