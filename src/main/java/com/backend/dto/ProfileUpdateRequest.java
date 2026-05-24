package com.backend.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class ProfileUpdateRequest {

    @Size(max = 500, message = "头像URL过长")
    private String avatar;

    @Size(max = 200, message = "个人简介最长200字")
    private String bio;

    private List<Integer> preferredTags;

    @Pattern(regexp = "low|middle|high", message = "预算等级仅限 low/middle/high")
    private String budgetLevel;
}
