package com.backend.service;

import com.backend.entity.TripMessage;
import com.backend.entity.TripSession;
import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

public interface TripChatService {

    TripSession createSession(Long userId, String city);

    String chat(Long sessionId, Long userId, String message);

    List<TripMessage> getMessages(Long sessionId);

    void saveMessages(Long sessionId, List<ChatMessage> messages);

    List<ChatMessage> buildMessages(Long sessionId, String userMessage);
}
