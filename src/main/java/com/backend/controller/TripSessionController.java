package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.ChatResponse;
import com.backend.entity.TripSession;
import com.backend.service.TripChatService;
import com.backend.service.TripSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trip-session")
public class TripSessionController {

    @Autowired
    private TripChatService chatService;

    @Autowired
    private TripSessionService sessionService;

    @PostMapping
    public Result<TripSession> create(@RequestBody Map<String, String> body,
                                      Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String city = body.getOrDefault("city", "广州");
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
        ChatResponse resp = chatService.chat(id, userId, message);
        return Result.success(resp);
    }

    @GetMapping("/{id}/messages")
    public Result<ChatResponse> messages(@PathVariable Long id) {
        return Result.success(chatService.getHistory(id));
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
}
