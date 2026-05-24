package com.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ActionRequest {

    @NotBlank
    @Pattern(regexp = "like|unlike|favorite|unfavorite", message = "action 仅限 like / unlike / favorite / unfavorite")
    private String action;
}
