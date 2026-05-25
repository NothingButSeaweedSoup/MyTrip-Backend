package com.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ItineraryRequest {

    @NotBlank(message = "城市不能为空")
    private String city = "广州";

    @Min(1) @Max(7)
    private int days = 2;

    private String budget = "middle";   // low / middle / high

    private List<String> interests;      // 自然风光/历史文化/美食/购物/亲子/夜生活

    private String pace = "moderate";    // relaxed / moderate / intense
}
