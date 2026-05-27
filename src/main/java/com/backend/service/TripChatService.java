package com.backend.service;

import com.backend.entity.TripMessage;
import com.backend.entity.TripSession;

import java.util.List;

public interface TripChatService {

    TripSession createSession(Long userId, String city);

    String chat(Long sessionId, Long userId, String message);

    List<TripMessage> getMessages(Long sessionId);
}
