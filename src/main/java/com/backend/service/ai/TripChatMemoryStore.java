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

        // 清空旧记录
        messageMapper.delete(new LambdaQueryWrapper<TripMessage>()
                .eq(TripMessage::getSessionId, sid));

        // 批量写入
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

    // --- 转换 ---

    private static Long toLong(Object id) {
        if (id == null) return null;
        try { return Long.valueOf(id.toString()); }
        catch (NumberFormatException e) { return null; }
    }

    private static ChatMessage fromRow(TripMessage row) {
        return switch (row.getRole()) {
            case "user" -> new UserMessage(row.getContent());
            case "ai" -> new AiMessage(row.getContent());
            case "tool" -> ToolExecutionResultMessage.from(
                    "", row.getToolName(), row.getToolResult());
            case "system" -> new SystemMessage(row.getContent());
            default -> null;
        };
    }

    private static TripMessage toRow(Long sessionId, ChatMessage msg) {
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
            case ToolExecutionResultMessage m -> {
                row.setRole("tool");
                row.setToolName(m.toolName());
                row.setToolResult(m.text());
                row.setContent("");
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
