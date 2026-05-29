package com.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class RecommendConfigUpdateRequest {
    @DecimalMin(value = "0", message = "权重最小0")
    @DecimalMax(value = "1", message = "权重最大1")
    private Double weightHot;

    @DecimalMin(value = "0", message = "权重最小0")
    @DecimalMax(value = "1", message = "权重最大1")
    private Double weightTag;

    @DecimalMin(value = "0", message = "权重最小0")
    @DecimalMax(value = "1", message = "权重最大1")
    private Double weightFresh;

    @DecimalMin(value = "0", message = "权重最小0")
    @DecimalMax(value = "1", message = "权重最大1")
    private Double weightDiversity;

    @Min(value = 10, message = "召回数量最小10")
    @Max(value = 1000, message = "召回数量最大1000")
    private Integer tagMatchLimit;

    @Min(value = 10, message = "召回数量最小10")
    @Max(value = 1000, message = "召回数量最大1000")
    private Integer hotLimit;

    @Min(value = 10, message = "召回数量最小10")
    @Max(value = 1000, message = "召回数量最大1000")
    private Integer itemCfLimit;
}
