package com.backend.service.impl;

import com.backend.dto.ChatResponse;
import com.backend.entity.TripPlan;
import com.backend.entity.TripSession;
import com.backend.service.TripChatService;
import com.backend.service.TripPlanService;
import com.backend.service.TripSessionService;
import com.backend.service.ai.ItineraryAiService;
import com.backend.service.ai.TripChatMemoryStore;
import com.backend.tool.AmapTool;
import com.backend.tool.DateTimeTool;
import com.backend.tool.LocationTool;
import com.backend.tool.PlanTool;
import com.backend.tool.WeatherTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class TripChatServiceImpl implements TripChatService {

    @Autowired
    @Qualifier("deepSeekChatModel")
    private ChatModel deepSeekChatModel;

    @Autowired
    private TripSessionService sessionService;

    @Autowired
    private TripPlanService planService;

    @Autowired
    private TripChatMemoryStore memoryStore;

    @Autowired
    private AmapTool amapTool;

    @Autowired
    private WeatherTool weatherTool;

    @Autowired
    private DateTimeTool dateTimeTool;

    @Autowired
    private LocationTool locationTool;

    @Autowired
    private PlanTool planTool;

    @Override
    public TripSession createSession(Long userId, String city) {
        TripSession session = new TripSession();
        session.setUserId(userId);
        session.setTitle(city != null ? "聊聊" + city : "新对话");
        sessionService.save(session);
        return session;
    }

    @Override
    public ChatResponse chat(Long sessionId, Long userId, String message) {
        TripSession session = sessionService.getById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }

        ItineraryAiService ai = AiServices.builder(ItineraryAiService.class)
                .chatModel(deepSeekChatModel)
                .chatMemory(MessageWindowChatMemory.builder()
                        .maxMessages(40)
                        .chatMemoryStore(memoryStore)
                        .id(sessionId)
                        .build())
                .tools(amapTool, weatherTool, dateTimeTool, planTool, locationTool)
                .build();

        String prompt = buildPrompt(message, session);

        String reply;
        try {
            reply = ai.generateItinerary(prompt);
        } catch (Exception e) {
            reply = "AI 响应出错: " + e.getMessage();
        }

        // 更新标题
        if (session.getTitle() == null || session.getTitle().startsWith("新对话")
                || session.getTitle().startsWith("聊聊")) {
            String t = message.length() > 30 ? message.substring(0, 30) + "…" : message;
            session.setTitle(t);
            sessionService.updateById(session);
        }

        // 检查是否刚生成了计划
        boolean hasPlan = session.getPlanId() != null;
        TripPlan plan = hasPlan ? planService.getById(session.getPlanId()) : null;

        return ChatResponse.builder()
                .sessionId(sessionId)
                .reply(reply)
                .planGenerated(hasPlan)
                .build();
    }

    @Override
    public ChatResponse getHistory(Long sessionId) {
        var messages = memoryStore.getMessages(sessionId);
        String text = messages.stream()
                .map(m -> switch (m.type()) {
                    case USER -> "👤 " + textOf(m);
                    case AI -> "🤖 " + textOf(m);
                    case TOOL_EXECUTION_RESULT -> "🔧 " + textOf(m);
                    default -> "";
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n"));

        return ChatResponse.builder()
                .sessionId(sessionId)
                .reply(text.isEmpty() ? "暂无对话" : text)
                .planGenerated(false)
                .build();
    }

    private String buildPrompt(String userMsg, TripSession session) {
        String city = resolveCity(session);
        return String.format("""
                当前上下文 — 会话ID: %d, 城市: %s, 已有计划ID: %s

                用户: %s

                （你可以使用以下工具：
                - generatePlan: 生成行程计划
                - getDailyForecast: 查天气
                - geocode/transitRoute/walkingRoute/drivingDistance: 查交通
                根据用户需求自主决策是否需要调用工具。）
                """, session.getSessionId(), city,
                session.getPlanId() != null ? session.getPlanId().toString() : "无",
                userMsg);
    }

    private String resolveCity(TripSession session) {
        if (session.getPlanId() != null) {
            TripPlan plan = planService.getById(session.getPlanId());
            if (plan != null && plan.getDestination() != null) return plan.getDestination();
        }
        return "广州";
    }

    private static String textOf(dev.langchain4j.data.message.ChatMessage m) {
        return switch (m) {
            case dev.langchain4j.data.message.AiMessage ai -> ai.text();
            case dev.langchain4j.data.message.UserMessage um ->
                    um.hasSingleText() ? um.singleText() : "[复合消息]";
            case dev.langchain4j.data.message.ToolExecutionResultMessage tr -> tr.text();
            default -> "";
        };
    }
}
