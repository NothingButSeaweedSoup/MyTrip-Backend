package com.backend.service.impl;

import com.backend.config.AiPrompts;
import com.backend.entity.TripMessage;
import com.backend.entity.TripSession;
import com.backend.service.TripChatService;
import com.backend.service.TripMessageService;
import com.backend.service.TripSessionService;
import com.backend.service.ai.TripAgentService;
import com.backend.service.ai.TripChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TripChatServiceImpl implements TripChatService {

    private static final Logger log = LoggerFactory.getLogger(TripChatServiceImpl.class);

    @Autowired
    private TripAgentService agentService;
    @Autowired
    private TripSessionService sessionService;
    @Autowired
    private TripMessageService messageService;
    @Autowired
    private TripChatMemoryStore memoryStore;

    @Override
    public TripSession createSession(Long userId, String city) {
        TripSession s = new TripSession();
        s.setUserId(userId);
        s.setTitle(city != null && !city.isBlank() ? "聊聊" + city : "新对话");
        sessionService.save(s);
        return s;
    }

    @Override
    public String chat(Long sessionId, Long userId, String message) {
        TripSession session = sessionService.getById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }

        List<ChatMessage> messages = buildMessages(sessionId, message);
        String reply = agentService.run(messages);

        // 保存本轮消息到持久化记忆（system/tool 消息由 TripChatMemoryStore 自动过滤）
        memoryStore.updateMessages(sessionId, messages);

        log.info("[Chat] session={}, reply length={}", sessionId,
                reply != null ? reply.length() : 0);
        return reply;
    }

    @Override
    public List<TripMessage> getMessages(Long sessionId) {
        return messageService.listBySession(sessionId);
    }

    // --- package-private for controller ---

    public void saveMessages(Long sessionId, List<ChatMessage> messages) {
        memoryStore.updateMessages(sessionId, messages);
    }

    public List<ChatMessage> buildMessages(Long sessionId, String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        // 注入当前日期 + 会话信息
        TripSession session = sessionService.getById(sessionId);
        String dateInfo = java.time.ZonedDateTime
                .now(java.time.ZoneId.of("Asia/Shanghai"))
                .format(java.time.format.DateTimeFormatter.ofPattern("今天是yyyy年MM月dd日 EEEE"));
        String sessionInfo = (session != null)
                ? "当前会话标题: " + session.getTitle() + " (sessionId=" + sessionId + ")"
                : "sessionId=" + sessionId;
        messages.add(SystemMessage.from(AiPrompts.TRIP_PLANNER + "\n" + dateInfo + "\n" + sessionInfo));

        // 加载历史消息（最多 10 条，跳过 system/tool）
        List<ChatMessage> history = memoryStore.getMessages(sessionId);
        int from = Math.max(0, history.size() - 10);
        messages.addAll(history.subList(from, history.size()));

        messages.add(UserMessage.from(userMessage));
        return messages;
    }
}
