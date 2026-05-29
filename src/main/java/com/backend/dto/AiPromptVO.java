package com.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class AiPromptVO {
    private String prompt;
    private String defaultPrompt;
    private Date lastUpdateTime;
}
