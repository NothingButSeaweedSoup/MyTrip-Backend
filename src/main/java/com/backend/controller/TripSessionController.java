package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.ChatResponse;
import com.backend.dto.ItineraryVO;
import com.backend.dto.ItineraryVO.DayPlan;
import com.backend.dto.ItineraryVO.LocationItem;
import com.backend.dto.ItineraryVO.SpotItem;
import com.backend.entity.TripMessage;
import com.backend.entity.TripPlan;
import com.backend.entity.TripPlanLocation;
import com.backend.entity.TripSession;
import com.backend.mapper.TripPlanLocationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.backend.service.TripChatService;
import com.backend.service.TripPlanService;
import com.backend.service.TripSessionService;
import com.backend.service.ai.TripAgentService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/trip-session")
public class TripSessionController {

    private static final Logger log = LoggerFactory.getLogger(TripSessionController.class);

    @Autowired
    private TripChatService chatService;
    @Autowired
    private com.backend.service.impl.TripChatServiceImpl chatServiceImpl;
    @Autowired
    private TripSessionService sessionService;
    @Autowired
    private TripPlanService planService;
    @Autowired
    private TripAgentService agentService;
    @Autowired
    private TripPlanLocationMapper locationMapper;

    @PostMapping
    public Result<TripSession> create(@RequestBody Map<String, String> body,
                                      Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String city = body.getOrDefault("city", "广州");
        // 支持 cities 逗号分隔多城市
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

        ItineraryVO plan = loadPlan(id);
        logPlanDetection(id, plan);

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
                var messages = chatServiceImpl.buildMessages(id, message);
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
                        ItineraryVO plan = loadPlan(id);
                        if (plan != null) {
                            sendEvent(emitter, "plan", "{\"planId\":" + plan.getPlanId()
                                    + ",\"title\":\"" + jsonStr(plan.getTitle()) + "\"}");
                        }
                        sendEvent(emitter, "done", "{}");
                        emitter.complete();
                    }
                });
                chatServiceImpl.saveMessages(id, messages);
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
        ItineraryVO plan = loadPlan(id);
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

    private ItineraryVO loadPlan(Long sessionId) {
        TripSession s = sessionService.getById(sessionId);
        if (s == null || s.getPlanId() == null) {
            log.info("[PlanDetect] sessionId={} hasPlanId=null", sessionId);
            return null;
        }
        TripPlan tp = planService.getById(s.getPlanId());
        if (tp == null) {
            log.info("[PlanDetect] planId={} 不存在", s.getPlanId());
            return null;
        }

        List<DayPlan> itinerary = buildItineraryFromLocations(s.getPlanId());
        String weather = tp.getWeatherInfo();
        List<LocationItem> locations = loadLocations(s.getPlanId());

        log.info("[PlanDetect] ✓ 加载计划 planId={} title={} days={} itineraryDays={} locations={}",
                tp.getPlanId(), tp.getTitle(), tp.getDays(),
                itinerary != null ? itinerary.size() : 0,
                locations != null ? locations.size() : 0);

        return ItineraryVO.builder()
                .planId(tp.getPlanId())
                .title(tp.getTitle())
                .days(tp.getDays())
                .budget(tp.getBudget())
                .weather(weather)
                .itinerary(itinerary)
                .locations(locations)
                .build();
    }

    private List<LocationItem> loadLocations(Long planId) {
        LambdaQueryWrapper<TripPlanLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripPlanLocation::getPlanId, planId)
                .orderByAsc(TripPlanLocation::getSortOrder);
        List<TripPlanLocation> locs = locationMapper.selectList(wrapper);
        return locs.stream().map(l -> LocationItem.builder()
                .locationId(l.getLocationId())
                .name(l.getName())
                .city(l.getCity())
                .address(l.getAddress())
                .latitude(l.getLatitude())
                .longitude(l.getLongitude())
                .dayNumber(l.getDayNumber())
                .sortOrder(l.getSortOrder())
                .timeSlot(l.getTimeSlot())
                .duration(l.getDuration())
                .transport(l.getTransport())
                .build()).collect(Collectors.toList());
    }

    private List<DayPlan> buildItineraryFromLocations(Long planId) {
        LambdaQueryWrapper<TripPlanLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripPlanLocation::getPlanId, planId)
                .isNotNull(TripPlanLocation::getDayNumber)
                .orderByAsc(TripPlanLocation::getSortOrder);
        List<TripPlanLocation> locs = locationMapper.selectList(wrapper);

        Map<Integer, List<TripPlanLocation>> dayMap = locs.stream()
                .collect(Collectors.groupingBy(TripPlanLocation::getDayNumber));

        return dayMap.keySet().stream().sorted().map(day -> {
            List<SpotItem> spots = dayMap.get(day).stream().map(l -> SpotItem.builder()
                    .timeSlot(l.getTimeSlot())
                    .name(l.getName())
                    .address(l.getAddress())
                    .duration(l.getDuration())
                    .transport(l.getTransport())
                    .note(l.getDescription())
                    .lat(l.getLatitude())
                    .lng(l.getLongitude())
                    .build()).collect(Collectors.toList());
            return DayPlan.builder()
                    .day(day)
                    .date("第" + day + "天")
                    .spots(spots)
                    .build();
        }).collect(Collectors.toList());
    }

    private void sendEvent(SseEmitter emitter, String event, String data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (IOException ignored) {}
    }

    private void logPlanDetection(Long sessionId, ItineraryVO plan) {
        TripSession s = sessionService.getById(sessionId);
        log.info("[PlanDetect] sessionId={} hasPlanId={} loadResult={}",
                sessionId, s != null ? s.getPlanId() : "N/A",
                plan != null ? "planId=" + plan.getPlanId() + " days=" + plan.getDays() : "null");
    }

    private String jsonStr(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
