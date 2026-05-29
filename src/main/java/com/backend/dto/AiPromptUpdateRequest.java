package com.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiPromptUpdateRequest {
    @NotBlank(message = "提示词不能为空")
    @Size(max = 5000, message = "提示词最长5000字")
    private String prompt;
}
