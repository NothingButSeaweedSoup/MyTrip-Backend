package com.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private Long sessionId;
    private String reply;
    private ItineraryVO plan;       // 本次对话新生成的计划（可空）
    private boolean planGenerated;
}
