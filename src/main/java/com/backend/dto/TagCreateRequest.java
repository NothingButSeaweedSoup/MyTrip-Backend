package com.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagCreateRequest {
    @NotBlank(message = "标签名不能为空")
    @Size(max = 50, message = "标签名最长50字")
    private String name;
}
