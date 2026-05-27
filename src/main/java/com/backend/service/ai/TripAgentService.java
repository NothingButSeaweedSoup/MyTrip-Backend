package com.backend.service.ai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripAgentService {

    private static final Logger log = LoggerFactory.getLogger(TripAgentService.class);

    private final ChatModel chatModel;
    private final Object toolService;
    private final List<ToolSpecification> toolSpecs;
    private final int maxRounds;

    public TripAgentService(
            ChatModel chatModel,
            com.backend.tool.ToolService toolService,
            @Value("${ai.agent.max-tool-rounds:12}") int maxRounds) {
        this.chatModel = chatModel;
        this.toolService = toolService;
        this.toolSpecs = ToolSpecifications.toolSpecificationsFrom(toolService);
        ToolSpecifications.validateSpecifications(this.toolSpecs);
        this.maxRounds = maxRounds;
        log.info("[Agent] 初始化完成, 工具数={}, 最大轮次={}",
                this.toolSpecs.size(), maxRounds);
    }

    public interface TraceListener {
        default void onRoundStart(int round, int maxRounds) {}
        default void onToolCall(String toolName, String arguments) {}
        default void onToolResult(String toolName, String resultPreview) {}
        default void onFinalAnswer(String answer) {}
    }

    public String run(List<ChatMessage> messages) {
        return run(messages, null);
    }

    public String run(List<ChatMessage> messages, TraceListener listener) {
        log.info("[Agent] 开始 ReAct 循环, 初始消息数={}", messages.size());

        for (int round = 0; round < maxRounds; round++) {
            log.info("[Agent] === round {}/{} (当前消息数={}) ===",
                    round + 1, maxRounds, messages.size());
            if (listener != null) listener.onRoundStart(round + 1, maxRounds);

            ChatRequest request = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(toolSpecs)
                    .build();
            ChatResponse response = chatModel.chat(request);
            AiMessage ai = response.aiMessage();
            messages.add(ai);

            log.info("[Agent] round {} AiMessage: hasText={}, textPreview={}, toolRequests={}",
                    round + 1,
                    ai.text() != null && !ai.text().isEmpty(),
                    ai.text() != null ? ai.text().substring(0, Math.min(ai.text().length(), 100)) : "<null>",
                    ai.hasToolExecutionRequests() ? ai.toolExecutionRequests().size() : 0);

            if (!ai.hasToolExecutionRequests()) {
                String text = ai.text() != null ? ai.text() : "";
                log.info("[Agent] ✓ 第{}轮获得最终文本回答 (长度={})", round + 1, text.length());
                if (listener != null) listener.onFinalAnswer(text);
                return text;
            }

            for (ToolExecutionRequest req : ai.toolExecutionRequests()) {
                log.info("[Agent] → 调用工具: {} 参数: {}", req.name(), req.arguments());
                if (listener != null) listener.onToolCall(req.name(), req.arguments());

                String result;
                try {
                    DefaultToolExecutor executor = new DefaultToolExecutor(toolService, req);
                    result = executor.execute(req, null);
                } catch (Exception e) {
                    log.error("[Agent] ✗ 工具 {} 执行异常: {}", req.name(), e.getMessage(), e);
                    result = "工具执行失败: " + e.getMessage();
                }

                log.info("[Agent] ← 工具 {} 返回 (长度={}, 预览={})",
                        req.name(), result.length(),
                        result.length() > 150 ? result.substring(0, 150) + "…" : result);

                String preview = result.length() > 200 ? result.substring(0, 200) + "…" : result;
                if (listener != null) listener.onToolResult(req.name(), preview);
                messages.add(ToolExecutionResultMessage.from(req, result));
            }
        }

        log.warn("[Agent] 超过最大轮次 {}，强制无工具生成最终回答", maxRounds);
        ChatRequest finalReq = ChatRequest.builder().messages(messages).build();
        String finalText = chatModel.chat(finalReq).aiMessage().text();
        if (listener != null) listener.onFinalAnswer(finalText != null ? finalText : "");
        log.info("[Agent] 强制回答 (长度={})", finalText != null ? finalText.length() : 0);
        return finalText != null ? finalText : "抱歉，处理超时，请重新提问。";
    }
}
