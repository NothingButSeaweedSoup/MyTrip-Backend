package com.backend.service.ai;

import com.backend.entity.TripMessage;
import com.backend.mapper.TripMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TripChatMemoryStore implements ChatMemoryStore {

    private final TripMessageMapper messageMapper;

    public TripChatMemoryStore(TripMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Long sid = toLong(memoryId);
        if (sid == null) return List.of();

        List<TripMessage> rows = messageMapper.selectBySessionId(sid);
        List<ChatMessage> messages = new ArrayList<>();
        for (TripMessage row : rows) {
            ChatMessage msg = fromRow(row);
            if (msg != null) messages.add(msg);
        }
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        Long sid = toLong(memoryId);
        if (sid == null) return;

        messageMapper.delete(new LambdaQueryWrapper<TripMessage>()
                .eq(TripMessage::getSessionId, sid));

        for (ChatMessage msg : messages) {
            TripMessage row = toRow(sid, msg);
            if (row != null) messageMapper.insert(row);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Long sid = toLong(memoryId);
        if (sid == null) return;
        messageMapper.delete(new LambdaQueryWrapper<TripMessage>()
                .eq(TripMessage::getSessionId, sid));
    }

    private static Long toLong(Object id) {
        if (id == null) return null;
        try { return Long.valueOf(id.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private static ChatMessage fromRow(TripMessage row) {
        return switch (row.getRole()) {
            case "user" -> new UserMessage(row.getContent());
            case "ai" -> new AiMessage(row.getContent());
            case "system" -> new SystemMessage(row.getContent());
            default -> null;
        };
    }

    private static TripMessage toRow(Long sessionId, ChatMessage msg) {
        // 跳过系统消息——它是 prompt 模板，不是对话内容
        if (msg instanceof SystemMessage) return null;
        // 跳过工具消息——内部 tool_calls 元数据无法无损持久化
        if (msg instanceof ToolExecutionResultMessage) return null;
        if (msg instanceof AiMessage ai && ai.hasToolExecutionRequests()) return null;

        TripMessage row = new TripMessage();
        row.setSessionId(sessionId);
        switch (msg) {
            case UserMessage m -> {
                row.setRole("user");
                row.setContent(m.hasSingleText() ? m.singleText() : "[非文本消息]");
            }
            case AiMessage m -> {
                row.setRole("ai");
                row.setContent(m.text());
            }
            case SystemMessage m -> {
                row.setRole("system");
                row.setContent(m.text());
            }
            default -> { return null; }
        }
        return row;
    }
}
