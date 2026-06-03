package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.ChatResponse;
import com.backend.dto.ItineraryVO;
import com.backend.entity.TripMessage;
import com.backend.entity.TripSession;
import com.backend.service.TripChatService;
import com.backend.service.TripPlanService;
import com.backend.service.TripSessionService;
import com.backend.service.ai.TripAgentService;
import dev.langchain4j.data.message.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trip-session")
public class TripSessionController {

    private static final Logger log = LoggerFactory.getLogger(TripSessionController.class);

    @Autowired
    private TripChatService chatService;
    @Autowired
    private TripSessionService sessionService;
    @Autowired
    private TripPlanService planService;
    @Autowired
    private TripAgentService agentService;

    @PostMapping
    public Result<TripSession> create(@RequestBody Map<String, String> body,
                                      Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String city = body.getOrDefault("city", "广州");
        city = body.getOrDefault("cities", city);
        TripSession session = chatService.createSession(userId, city);
        return Result.success(session);
    }

    @PostMapping("/{id}/chat")
    public Result<ChatResponse> chat(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return Result.error("消息不能为空");
        }
        String reply = chatService.chat(id, userId, message);
        List<TripMessage> messages = chatService.getMessages(id);

        ItineraryVO plan = planService.getPlanBySessionId(id);

        return Result.success(ChatResponse.builder()
                .sessionId(id)
                .reply(reply)
                .plan(plan)
                .messages(messages)
                .build());
    }

    @PostMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id,
                             @RequestBody Map<String, String> body,
                             Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            SseEmitter err = new SseEmitter();
            err.completeWithError(new IllegalArgumentException("消息不能为空"));
            return err;
        }

        TripSession session = sessionService.getById(id);
        if (session == null || !session.getUserId().equals(userId)) {
            SseEmitter err = new SseEmitter();
            err.completeWithError(new RuntimeException("会话不存在或无权访问"));
            return err;
        }

        SseEmitter emitter = new SseEmitter(120_000L);

        new Thread(() -> {
            try {
                List<ChatMessage> messages = chatService.buildMessages(id, message);
                agentService.run(messages, new TripAgentService.TraceListener() {
                    @Override
                    public void onToolCall(String toolName, String arguments) {
                        sendEvent(emitter, "tool_call",
                                "{\"tool\":\"" + toolName + "\",\"args\":" + jsonStr(arguments) + "}");
                    }

                    @Override
                    public void onToolResult(String toolName, String resultPreview) {
                        sendEvent(emitter, "tool_result",
                                "{\"tool\":\"" + toolName + "\",\"preview\":\"" + jsonStr(resultPreview) + "\"}");
                    }

                    @Override
                    public void onFinalAnswer(String answer) {
                        for (int i = 0; i < answer.length(); i += 3) {
                            int end = Math.min(i + 3, answer.length());
                            sendEvent(emitter, "answer_token", answer.substring(i, end));
                            try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                        }
                        ItineraryVO plan = planService.getPlanBySessionId(id);
                        if (plan != null) {
                            sendEvent(emitter, "plan", "{\"planId\":" + plan.getPlanId()
                                    + ",\"title\":\"" + jsonStr(plan.getTitle()) + "\"}");
                        }
                        sendEvent(emitter, "done", "{}");
                        emitter.complete();
                    }
                });
                chatService.saveMessages(id, messages);
            } catch (Exception e) {
                sendEvent(emitter, "error", "{\"message\":\"" + jsonStr(e.getMessage()) + "\"}");
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    @GetMapping("/{id}/messages")
    public Result<List<TripMessage>> messages(@PathVariable Long id) {
        return Result.success(chatService.getMessages(id));
    }

    @GetMapping("/{id}/plan")
    public Result<ItineraryVO> getPlan(@PathVariable Long id) {
        ItineraryVO plan = planService.getPlanBySessionId(id);
        return plan != null ? Result.success(plan) : Result.success(null);
    }

    @GetMapping("/my")
    public Result<List<TripSession>> mySessions(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.success(sessionService.listByUser(userId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        TripSession session = sessionService.getById(id);
        if (session == null) return Result.error("会话不存在");
        if (!session.getUserId().equals(userId)) return Result.error("无权删除");
        sessionService.removeById(id);
        return Result.success();
    }

    // --- helpers ---

    private void sendEvent(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException ignored) {}
    }

    private String jsonStr(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
