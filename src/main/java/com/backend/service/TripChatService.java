package com.backend.service;

import com.backend.dto.ChatResponse;
import com.backend.entity.TripSession;

public interface TripChatService {

    TripSession createSession(Long userId, String city);

    ChatResponse chat(Long sessionId, Long userId, String message);

    ChatResponse getHistory(Long sessionId);
}
