package com.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LikeRequest {

    @NotBlank
    @Pattern(regexp = "like|unlike", message = "action 仅限 like / unlike")
    private String action;
}
