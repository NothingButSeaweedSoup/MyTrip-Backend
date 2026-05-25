package com.backend.service.impl;

import com.backend.dto.ItineraryRequest;
import com.backend.dto.ItineraryVO;
import com.backend.dto.ItineraryVO.DayPlan;
import com.backend.dto.ItineraryVO.SpotItem;
import com.backend.entity.ScenicSpot;
import com.backend.entity.TripPlan;
import com.backend.entity.TripSession;
import com.backend.mapper.TripPlanMapper;
import com.backend.service.ScenicSpotService;
import com.backend.service.TripPlanService;
import com.backend.service.TripSessionService;
import com.backend.service.ai.ItineraryAiService;
import com.backend.tool.AmapTool;
import com.backend.tool.DateTimeTool;
import com.backend.tool.LocationTool;
import com.backend.tool.WeatherTool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TripPlanServiceImpl extends ServiceImpl<TripPlanMapper, TripPlan>
        implements TripPlanService {

    @Autowired
    @Qualifier("deepSeekChatModel")
    private ChatModel deepSeekChatModel;

    @Autowired
    private ScenicSpotService spotService;

    @Autowired
    private AmapTool amapTool;

    @Autowired
    private WeatherTool weatherTool;

    @Autowired
    private DateTimeTool dateTimeTool;

    @Autowired
    private LocationTool locationTool;

    @Autowired
    private TripSessionService sessionService;

    private final ObjectMapper json = new ObjectMapper();

    @Override
    public ItineraryVO generatePlan(Long userId, ItineraryRequest req) {
        String interests = req.getInterests() != null
                ? String.join("、", req.getInterests()) : "不限";
        return doGeneratePlan(0L, userId, req.getCity(), req.getDays(),
                req.getBudget(), interests, req.getPace());
    }

    @Override
    public String generatePlanWithAI(long sessionId, String city, int days,
                                     String budget, String interests, String pace) {
        TripSession session = sessionService.getById(sessionId);
        Long userId = session != null ? session.getUserId() : 0L;
        ItineraryVO vo = doGeneratePlan(sessionId, userId, city, days, budget, interests, pace);
        return vo != null
                ? String.format("已生成%d天%s行程 (计划ID=%d，共%d天%d个景点)",
                    days, city, vo.getPlanId(), vo.getDays(),
                    vo.getItinerary().stream().mapToInt(d -> d.getSpots().size()).sum())
                : "计划生成失败";
    }

    private ItineraryVO doGeneratePlan(long sessionId, Long userId,
                                        String city, int days, String budget,
                                        String interests, String pace) {
        List<ScenicSpot> spots = spotService.listByCity(city);
        if (spots.isEmpty()) {
            throw new RuntimeException("该城市暂无景点数据");
        }

        String prompt = buildPlanPrompt(city, days, budget, interests, pace, spots);

        ItineraryAiService ai = AiServices.builder(ItineraryAiService.class)
                .chatModel(deepSeekChatModel)
                .tools(amapTool, weatherTool, dateTimeTool, locationTool)
                .build();

        String result = ai.generateItinerary(prompt);
        List<DayPlan> itinerary = parseItinerary(result);
        String weather = extractWeather(result);

        TripPlan plan = new TripPlan();
        plan.setUserId(userId);
        plan.setDestination(city);
        plan.setDays(days);
        plan.setBudget(budget);
        plan.setPreferences(interests);
        plan.setItinerary(itinerary);
        plan.setWeatherInfo(weather);
        save(plan);

        // 关联会话
        if (sessionId > 0) {
            TripSession session = sessionService.getById(sessionId);
            if (session != null && session.getPlanId() == null) {
                session.setPlanId(plan.getPlanId());
                sessionService.updateById(session);
            }
        }

        return ItineraryVO.builder()
                .planId(plan.getPlanId())
                .destination(city)
                .days(days)
                .budget(budget)
                .weather(weather)
                .itinerary(itinerary)
                .build();
    }

    private String buildPlanPrompt(String city, int days, String budget,
                                     String interests, String pace, List<ScenicSpot> spots) {
        String budgetLabel = switch (budget) {
            case "low" -> "穷游/经济型";
            case "high" -> "高端/豪华型";
            default -> "舒适/中等";
        };
        String paceDesc = switch (pace) {
            case "relaxed" -> "轻松休闲，每天2-3个景点";
            case "intense" -> "紧凑充实，每天4-5个景点";
            default -> "适中，每天3-4个景点";
        };

        String spotsJson = spots.stream().map(s -> String.format(
                "{\"id\":%d,\"name\":\"%s\",\"address\":\"%s\",\"lat\":%s,\"lng\":%s,"
                        + "\"rating\":%s,\"duration\":%s,\"openTime\":\"%s\","
                        + "\"tags\":%s,\"desc\":\"%s\"}",
                s.getSpotId(),
                escape(s.getName()), escape(s.getAddress()),
                s.getLatitude(), s.getLongitude(),
                s.getRating(), s.getVisitDuration(),
                escape(s.getOpenTime()),
                s.getTags(), escape(s.getDescription())
        )).collect(Collectors.joining(",\n"));

        return String.format("""
                你是一个专业的旅游规划师。请为用户规划%d天%s行程。

                ## 偏好
                预算: %s | 兴趣: %s | 节奏: %s

                ## 可用工具
                WeatherTool.getDailyForecast / AmapTool.geocode/transitRoute/walkingRoute/drivingDistance

                ## 规则
                - 同日景点在同一区域，避免东西来回跑
                - 先查天气，雨天优先室内景点
                - 景点间用 transitRoute 查交通
                - 上午景点开园早，晚上考虑夜游景点
                - 吃饭时间穿插推荐餐厅
                - 景点游玩时长参考duration（分钟）

                ## 可选景点
                %s

                ## 输出JSON（不要markdown）:
                {"weather":"天气摘要","itinerary":[{"day":1,"date":"第1天","weather":"当天","spots":[{"timeSlot":"上午","name":"...","address":"...","duration":"2小时","transport":"地铁30分钟","note":"..."}]}]}
                """,
                days, city, budgetLabel, interests, paceDesc, spotsJson);
    }

    private List<DayPlan> parseItinerary(String result) {
        try {
            // 提取 JSON 块（可能包裹在 markdown 代码块中）
            String jsonStr = result;
            int start = result.indexOf('{');
            int end = result.lastIndexOf('}');
            if (start >= 0 && end > start) {
                jsonStr = result.substring(start, end + 1);
            }
            var node = json.readTree(jsonStr);
            var days = node.get("itinerary");
            if (days == null) return List.of();
            return json.convertValue(days, new TypeReference<List<DayPlan>>() {});
        } catch (Exception e) {
            // 解析失败，返回包含原始结果的单个条目
            DayPlan fallback = DayPlan.builder()
                    .day(1).date("第1天").weather("")
                    .spots(List.of(SpotItem.builder()
                            .timeSlot("全天").name("行程生成中")
                            .note(result).build()))
                    .build();
            return List.of(fallback);
        }
    }

    private String extractWeather(String result) {
        try {
            int start = result.indexOf("\"weather\"");
            if (start >= 0) {
                int valStart = result.indexOf('"', result.indexOf(':', start) + 1);
                int valEnd = result.indexOf('"', valStart + 1);
                if (valStart > 0 && valEnd > valStart) {
                    return result.substring(valStart + 1, valEnd);
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    @Override
    public List<TripPlan> listByUser(Long userId) {
        LambdaQueryWrapper<TripPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripPlan::getUserId, userId)
                .orderByDesc(TripPlan::getCreateTime);
        return list(wrapper);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", "");
    }
}
