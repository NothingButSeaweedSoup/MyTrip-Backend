package com.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Data;

import java.util.List;

@Data
public class ItineraryRequest {

    private List<String> cities;

    @Min(1) @Max(7)
    private int days = 2;

    private String budget = "middle";   // low / middle / high

    private List<String> interests;      // 自然风光/历史文化/美食/购物/亲子/夜生活

    private String pace = "moderate";    // relaxed / moderate / intense
}
