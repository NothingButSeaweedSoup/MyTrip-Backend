package com.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class RecommendConfigVO {
    private Double weightHot;
    private Double weightTag;
    private Double weightFresh;
    private Double weightDiversity;
    private Integer tagMatchLimit;
    private Integer hotLimit;
    private Integer itemCfLimit;
    private Date lastUpdateTime;
}
