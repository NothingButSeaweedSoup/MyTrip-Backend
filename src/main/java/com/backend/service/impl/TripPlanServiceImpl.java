package com.backend.service.impl;

import com.backend.util.DataCleaner;
import com.backend.dto.ItineraryRequest;
import com.backend.dto.ItineraryVO;
import com.backend.dto.ItineraryVO.DayPlan;
import com.backend.dto.ItineraryVO.SpotItem;
import com.backend.entity.ScenicSpot;
import com.backend.entity.TripPlan;
import com.backend.entity.TripPlanLocation;
import com.backend.entity.TripSession;
import com.backend.mapper.TripPlanLocationMapper;
import com.backend.mapper.TripPlanMapper;
import com.backend.service.ScenicSpotService;
import com.backend.service.TripPlanService;
import com.backend.service.TripSessionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TripPlanServiceImpl extends ServiceImpl<TripPlanMapper, TripPlan>
        implements TripPlanService {

    private static final Logger log = LoggerFactory.getLogger(TripPlanServiceImpl.class);

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private ScenicSpotService spotService;

    @Autowired
    private TripSessionService sessionService;

    @Autowired
    private TripPlanLocationMapper locationMapper;

    private final ObjectMapper json = new ObjectMapper();

    /** 暂存 B 生成的草案，供 A 校对后确认存库 */
    private final Map<Long, PlanProposal> pendingProposals = new ConcurrentHashMap<>();

    // ==================== REST API（保留，直接生成+存库） ====================

    @Override
    public ItineraryVO generatePlan(Long userId, ItineraryRequest req) {
        String interests = req.getInterests() != null
                ? String.join("、", req.getInterests()) : "不限";
        List<String> cities = req.getCities() != null && !req.getCities().isEmpty()
                ? req.getCities() : List.of("广州");
        return doGeneratePlan(0L, userId, cities, req.getDays(),
                req.getBudget(), interests, req.getPace());
    }

    @Override
    public String generatePlanWithAI(long sessionId, String cities, int days,
                                     String budget, String interests, String pace) {
        TripSession session = sessionService.getById(sessionId);
        Long userId = session != null ? session.getUserId() : 0L;
        List<String> cityList = parseCities(cities);
        ItineraryVO vo = doGeneratePlan(sessionId, userId, cityList, days, budget, interests, pace);
        if (vo != null) {
            String cityLabel = String.join("、", cityList);
            log.info("[PlanGen] 创建行程成功 sessionId={} userId={} city={} days={} budget={} interests={} pace={}",
                    sessionId, userId, cityLabel, days, budget, interests, pace);
            return String.format("已生成%d天%s行程，共%d天%d个景点",
                    days, cityLabel, vo.getDays(),
                    vo.getItinerary().stream().mapToInt(d -> d.getSpots() != null ? d.getSpots().size() : 0).sum());
        }
        log.error("[PlanGen] 创建行程失败 sessionId={} userId={} city={} days={} budget={} interests={} pace={}",
                sessionId, userId, cityList, days, budget, interests, pace);
        return "计划生成失败";
    }

    // ==================== 双Agent流程: B生成草案 → A校对 → 确认存库 ====================

    @Override
    public String proposePlan(long sessionId, String cities, int days,
                               String budget, String interests, String pace) {
        TripSession session = sessionService.getById(sessionId);
        Long userId = session != null ? session.getUserId() : 0L;
        List<String> cityList = parseCities(cities);
        String cityLabel = String.join("、", cityList);

        PlanProposal proposal = generateProposal(userId, cityList, days, budget, interests, pace);
        pendingProposals.put(sessionId, proposal);
        log.info("[Propose] B生成草案 sessionId={} userId={} city={} days={}", sessionId, userId, cityLabel, days);

        // 返回详细文本给A校对（A不得将此内容直接展示给用户）
        StringBuilder sb = new StringBuilder();
        sb.append("【B生成的行程草案，仅供你（A）校对，不要展示给用户】\n\n");
        sb.append(String.format("=== %d天%s（预算:%s 兴趣:%s 节奏:%s）===\n\n", days, cityLabel, budget, interests, pace));
        if (proposal.weather != null && !proposal.weather.isEmpty()) {
            sb.append("天气预报：").append(proposal.weather).append("\n\n");
        }
        if (proposal.itinerary != null) {
            for (DayPlan day : proposal.itinerary) {
                sb.append("第").append(day.getDay()).append("天");
                if (day.getDate() != null) sb.append(" ").append(day.getDate());
                if (day.getWeather() != null) sb.append(" 天气:").append(day.getWeather());
                sb.append("\n");
                if (day.getSpots() != null) {
                    for (SpotItem spot : day.getSpots()) {
                        sb.append("  - ").append(spot.getTimeSlot()).append(": ").append(spot.getName());
                        if (spot.getAddress() != null) sb.append(" (").append(spot.getAddress()).append(")");
                        if (spot.getDuration() != null) sb.append(" ").append(spot.getDuration());
                        sb.append("\n");
                    }
                }
                sb.append("\n");
            }
        }
        sb.append("=== 校对要求 ===\n");
        sb.append("请检查以上草案是否合理。如果满意，直接调用 confirmPlan 保存（你的回复只需说'已保存'，不要重复行程）。");
        sb.append("如果需要修改，调用 proposePlan 并说明修改要求。");
        sb.append("记住：绝对不要把草案内容写入你的文字回复中！");
        return sb.toString().trim();
    }

    @Override
    public String confirmPlan(long sessionId) {
        PlanProposal proposal = pendingProposals.remove(sessionId);
        if (proposal == null) {
            log.warn("[Confirm] 无待确认草案 sessionId={}", sessionId);
            return "没有待确认的行程计划，请先调用 proposePlan 生成草案。";
        }
        ItineraryVO vo = persistPlan(sessionId, proposal);
        int spotCount = vo.getItinerary() != null
                ? vo.getItinerary().stream().mapToInt(d -> d.getSpots() != null ? d.getSpots().size() : 0).sum()
                : 0;
        log.info("[Confirm] A确认保存 sessionId={} planId={} spots={}", sessionId, vo.getPlanId(), spotCount);
        return String.format("行程已确认保存（planId=%d），前端将自动加载正式行程。告诉用户'行程已安排好了'即可，不要重复列出景点。",
                vo.getPlanId());
    }

    // ==================== 内部方法 ====================

    private PlanProposal generateProposal(Long userId, List<String> cities,
                                           int days, String budget, String interests, String pace) {
        List<ScenicSpot> spots = new ArrayList<>();
        for (String city : cities) {
            List<ScenicSpot> citySpots = spotService.listByCity(city);
            log.info("[Propose] 查询到{}个景点 for city={}", citySpots.size(), city);
            spots.addAll(citySpots);
        }
        if (spots.isEmpty()) {
            String cityLabel = String.join("、", cities);
            log.error("[Propose] 城市{}无景点数据", cityLabel);
            throw new RuntimeException("所选城市暂无景点数据");
        }

        spots = spots.stream()
                .sorted(Comparator.comparing(ScenicSpot::getRating,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(60)
                .collect(Collectors.toList());

        String cityLabel = String.join("、", cities);
        String prompt = buildPlanPrompt(cityLabel, days, budget, interests, pace, spots);
        log.info("[Propose] 发送LLM请求 prompt长度={} spots={}", prompt.length(), spots.size());
        ChatRequest req = ChatRequest.builder()
                .messages(SystemMessage.from("你必须输出严格JSON，不要markdown包裹，不要额外文字"), UserMessage.from(prompt))
                .responseFormat(ResponseFormat.JSON)
                .build();

        String result;
        try {
            result = chatModel.chat(req).aiMessage().text();
        } catch (Exception e) {
            log.error("[Propose] LLM调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI行程生成失败，请稍后重试: " + e.getMessage());
        }
        log.info("[Propose] LLM返回 (长度={})", result != null ? result.length() : 0);

        List<DayPlan> itinerary = parseItinerary(result);
        String weather = extractWeather(DataCleaner.extractJson(result));

        PlanProposal p = new PlanProposal();
        p.userId = userId;
        p.cities = cities;
        p.cityLabel = cityLabel;
        p.days = days;
        p.budget = budget;
        p.interests = interests;
        p.pace = pace;
        p.itinerary = itinerary;
        p.weather = weather;
        log.info("[Propose] B行程草案生成完成 {}天 {}个景点", days,
                itinerary.stream().mapToInt(d -> d.getSpots() != null ? d.getSpots().size() : 0).sum());
        return p;
    }

    private ItineraryVO persistPlan(Long sessionId, PlanProposal proposal) {
        TripPlan plan = new TripPlan();
        plan.setUserId(proposal.userId);
        plan.setTitle(proposal.cityLabel + proposal.days + "日游");
        plan.setDays(proposal.days);
        plan.setBudget(proposal.budget);
        plan.setPreferences(proposal.interests);
        plan.setWeatherInfo(proposal.weather);
        save(plan);
        log.info("[Persist] 计划已保存 planId={}", plan.getPlanId());

        saveLocations(plan.getPlanId(), proposal.cities, proposal.itinerary);

        if (sessionId > 0) {
            TripSession session = sessionService.getById(sessionId);
            if (session != null) {
                Long oldPlanId = session.getPlanId();
                session.setPlanId(plan.getPlanId());
                sessionService.updateById(session);
                log.info("[Persist] 关联会话 sessionId={} → planId={} (原planId={})",
                        sessionId, plan.getPlanId(), oldPlanId);
            }
        }

        return ItineraryVO.builder()
                .planId(plan.getPlanId())
                .title(plan.getTitle())
                .days(proposal.days)
                .budget(proposal.budget)
                .weather(proposal.weather)
                .itinerary(proposal.itinerary)
                .locations(buildLocationItems(plan.getPlanId()))
                .build();
    }

    private ItineraryVO doGeneratePlan(long sessionId, Long userId,
                                        List<String> cities, int days, String budget,
                                        String interests, String pace) {
        PlanProposal proposal = generateProposal(userId, cities, days, budget, interests, pace);
        return persistPlan(sessionId, proposal);
    }

    private void saveLocations(Long planId, List<String> cities, List<DayPlan> itinerary) {
        int sortOrder = 0;
        for (String city : cities) {
            TripPlanLocation loc = new TripPlanLocation();
            loc.setPlanId(planId);
            loc.setName(city);
            loc.setCity(city);
            loc.setSortOrder(sortOrder++);
            log.info("[LocationInsert] 插入城市地点 planId={} name={} city={} sortOrder={}",
                    planId, city, city, sortOrder - 1);
            locationMapper.insert(loc);
        }

        if (itinerary != null) {
            for (DayPlan day : itinerary) {
                if (day.getSpots() == null) continue;
                for (SpotItem spot : day.getSpots()) {
                    TripPlanLocation loc = new TripPlanLocation();
                    loc.setPlanId(planId);
                    loc.setName(spot.getName());
                    loc.setAddress(spot.getAddress());
                    loc.setLatitude(spot.getLat());
                    loc.setLongitude(spot.getLng());
                    loc.setDayNumber(day.getDay());
                    loc.setSortOrder(sortOrder++);
                    loc.setTimeSlot(spot.getTimeSlot());
                    loc.setDuration(spot.getDuration());
                    loc.setTransport(spot.getTransport());
                    loc.setDescription(spot.getNote());
                    log.info("[LocationInsert] 插入景点地点 planId={} day={} sortOrder={} name={} address={} lat={} lng={} timeSlot={} duration={} transport={}",
                            planId, day.getDay(), sortOrder - 1, spot.getName(),
                            spot.getAddress(), spot.getLat(), spot.getLng(),
                            spot.getTimeSlot(), spot.getDuration(), spot.getTransport());
                    locationMapper.insert(loc);
                }
            }
        }
        log.info("[Persist] 保存{}个地点 planId={}", sortOrder, planId);
    }

    private List<ItineraryVO.LocationItem> buildLocationItems(Long planId) {
        LambdaQueryWrapper<TripPlanLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TripPlanLocation::getPlanId, planId)
                .orderByAsc(TripPlanLocation::getSortOrder);
        List<TripPlanLocation> locs = locationMapper.selectList(wrapper);
        return locs.stream().map(l -> ItineraryVO.LocationItem.builder()
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

    private List<String> parseCities(String cities) {
        if (cities == null || cities.isBlank()) return List.of("广州");
        return Arrays.stream(cities.split("[,，、]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String buildPlanPrompt(String cityLabel, int days, String budget,
                                     String interests, String pace, List<ScenicSpot> spots) {

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
                你是一位专业的广东旅行规划师。请为用户规划%d天%s行程。

                ## 用户偏好
                预算: %s | 兴趣: %s | 节奏: %s | 天数: %d天

                ## 规划规则
                - 根据景点坐标（lat/lng）判断邻近关系，同一天的景点lat差距<0.03且lng差距<0.04
                - 景点顺序按经纬度顺路排列，避免来回折返
                - 两个景点坐标差<0.01视为步行可达，0.01~0.03公交/地铁，>0.03建议打车
                - 考虑景点开放时间（openTime）和游玩时长（duration，单位分钟）
                - 上午安排开园早的景点，晚上安排有夜游的景点
                - 中午12:00-13:30穿插附近美食区域
                - transport字段填写具体交通方式：步行/地铁/公交/打车，并估算时间

                ## 可选景点（JSON数组，含坐标）
                %s

                ## 输出要求
                直接输出JSON（不要markdown代码块，不要```），每个spot必须包含lat/lng:
                {"weather":"根据季节预估天气简述","itinerary":[{"day":1,"date":"第1天","weather":"当天天气","spots":[{"timeSlot":"上午","name":"景点名","address":"地址","duration":"X小时","transport":"地铁约30分钟","note":"备注","lat":23.13,"lng":113.33}]}]}
                """,
                days, cityLabel, budget, interests, pace, days, spotsJson);
    }

    private List<DayPlan> parseItinerary(String result) {
        try {
            String jsonStr = DataCleaner.extractJson(result);
            var node = json.readTree(jsonStr);
            var days = node.get("itinerary");
            if (days == null) return List.of();
            return json.convertValue(days, new TypeReference<List<DayPlan>>() {});
        } catch (Exception e) {
            log.warn("[Propose] JSON解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    private String extractWeather(String cleanedJson) {
        try {
            var node = json.readTree(cleanedJson);
            var w = node.get("weather");
            return w != null ? w.asText() : "";
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

    // ==================== 内部数据结构 ====================

    public static class PlanProposal {
        Long userId;
        List<String> cities;
        String cityLabel;
        int days;
        String budget;
        String interests;
        String pace;
        List<DayPlan> itinerary;
        String weather;
    }
}
