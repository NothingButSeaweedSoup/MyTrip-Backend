package com.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagEditRequest {
    @Size(max = 50, message = "标签名最长50字")
    private String name;
}
