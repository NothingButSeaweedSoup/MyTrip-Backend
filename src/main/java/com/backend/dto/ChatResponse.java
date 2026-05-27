package com.backend.dto;

import lombok.Builder;
import lombok.Data;

import com.backend.entity.TripMessage;
import java.util.List;

@Data
@Builder
public class ChatResponse {
    private Long sessionId;
    private String reply;
    private ItineraryVO plan;
    private List<TripMessage> messages;
}
