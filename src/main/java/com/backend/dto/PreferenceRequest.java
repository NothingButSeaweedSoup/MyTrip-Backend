package com.backend.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.util.List;

@Data
public class PreferenceRequest {

    private List<Integer> preferredTags;

    @Pattern(regexp = "low|middle|high", message = "预算等级仅限 low/middle/high")
    private String budgetLevel;
}
