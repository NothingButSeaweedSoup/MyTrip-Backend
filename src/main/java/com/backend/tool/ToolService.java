package com.backend.tool;

import com.backend.entity.ScenicSpot;
import com.backend.entity.TripSession;
import com.backend.service.ScenicSpotService;
import com.backend.service.TripPlanService;
import com.backend.service.TripSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ToolService {

    private static final Logger log = LoggerFactory.getLogger(ToolService.class);

    @Autowired private ScenicSpotService spotService;
    @Autowired private TripSessionService sessionService;
    @Lazy @Autowired private TripPlanService planService;

    @Value("${amap.api-key}")
    private String amapKey;

    private final RestTemplate rest = createWeatherRestTemplate();

    private static RestTemplate createWeatherRestTemplate() {
        try {
            // 关闭证书吊销检查，解决 Windows CRYPT_E_REVOCATION_OFFLINE 错误
            System.setProperty("com.sun.security.enableCRLDP", "false");
            System.setProperty("com.sun.net.ssl.checkRevocation", "false");
        } catch (Exception ignored) {}
        return new RestTemplate();
    }

    // ==================== 天气 ====================

    private static final String WEATHER_BASE = "https://api.open-meteo.com/v1";

    private static final java.util.Map<Integer, String> WMO = java.util.Map.<Integer, String>ofEntries(
            java.util.Map.entry(0, "晴"), java.util.Map.entry(1, "晴"), java.util.Map.entry(2, "多云"),
            java.util.Map.entry(3, "阴"), java.util.Map.entry(45, "有雾"), java.util.Map.entry(48, "雾凇"),
            java.util.Map.entry(51, "毛毛雨"), java.util.Map.entry(53, "小雨"), java.util.Map.entry(55, "中雨"),
            java.util.Map.entry(61, "小雨"), java.util.Map.entry(63, "中雨"), java.util.Map.entry(65, "大雨"),
            java.util.Map.entry(71, "小雪"), java.util.Map.entry(73, "中雪"), java.util.Map.entry(75, "大雪"),
            java.util.Map.entry(80, "阵雨"), java.util.Map.entry(81, "中阵雨"), java.util.Map.entry(82, "强阵雨"),
            java.util.Map.entry(85, "小阵雪"), java.util.Map.entry(86, "大阵雪"),
            java.util.Map.entry(95, "雷暴"), java.util.Map.entry(96, "雷暴伴冰雹"), java.util.Map.entry(99, "强雷暴伴冰雹")
    );

    @Tool("查询指定坐标未来N天天气预报。广州中心 lat=23.13, lng=113.33")
    public String getDailyForecast(double lat, double lng, int days) {
        try {
            if (days < 1) days = 1; if (days > 7) days = 7;
            String url = WEATHER_BASE + "/forecast?latitude=" + lat + "&longitude=" + lng
                    + "&daily=temperature_2m_max,temperature_2m_min,weather_code,precipitation_probability_max"
                    + "&timezone=Asia/Shanghai&forecast_days=" + days;
            JsonNode root = rest.getForObject(url, JsonNode.class);
            if (root == null) return "天气数据不可用";
            JsonNode daily = root.path("daily");
            if (daily.isMissingNode() || !daily.has("time")) return "天气数据不可用";

            StringBuilder sb = new StringBuilder();
            int actualDays = Math.min(days, daily.path("time").size());
            for (int i = 0; i < actualDays; i++) {
                int code = daily.path("weather_code").get(i).asInt();
                double tMin = daily.path("temperature_2m_min").get(i).asDouble();
                double tMax = daily.path("temperature_2m_max").get(i).asDouble();
                int precip = daily.path("precipitation_probability_max").get(i).asInt();
                String date = daily.path("time").get(i).asText();
                sb.append(String.format("%s: %s, %.0f~%.0f℃, 降水概率%d%%\n",
                        date, WMO.getOrDefault(code, "未知(" + code + ")"), tMin, tMax, precip));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("[Tool:Weather] 查询失败 lat={} lng={} days={}: {}", lat, lng, days, e.getMessage());
            return "天气查询失败: " + e.getMessage();
        }
    }

    // ==================== 景点 ====================

    @Tool("查询景点列表，返回名称、评分、游玩时长、开放时间、坐标。用于推荐景点、查找特定类型景点")
    public String querySpots(
            @P("城市名，如广州") String city,
            @P("名称关键词模糊匹配") String keyword,
            @P("标签如亲子/历史文化/自然风光/美食") String tag,
            @P("最低评分0-5") Double minRating,
            @P("最多返回条数") Integer limit) {
        List<ScenicSpot> all = spotService.listByCity(city);
        if (all.isEmpty()) return "该城市暂无景点数据";
        var stream = all.stream();
        if (keyword != null && !keyword.isBlank())
            stream = stream.filter(s -> s.getName() != null && s.getName().contains(keyword));
        if (tag != null && !tag.isBlank())
            stream = stream.filter(s -> s.getTags() != null && s.getTags().toString().contains(tag));
        if (minRating != null)
            stream = stream.filter(s -> s.getRating() != null
                    && s.getRating().doubleValue() >= minRating);
        int n = limit != null && limit > 0 ? Math.min(limit, 30) : 20;
        var result = stream.sorted(Comparator.comparing(ScenicSpot::getRating,
                        Comparator.nullsLast(Comparator.reverseOrder()))).limit(n).toList();
        if (result.isEmpty()) return "未找到匹配的景点";
        return result.stream().map(s -> String.format("%s | %.1f分 | %s分钟 | %s | lat=%.4f,lng=%.4f | %s",
                s.getName(), s.getRating() != null ? s.getRating() : 0,
                s.getVisitDuration() != null ? s.getVisitDuration() : 0,
                s.getOpenTime() != null ? s.getOpenTime() : "?",
                s.getLatitude() != null ? s.getLatitude() : 0,
                s.getLongitude() != null ? s.getLongitude() : 0,
                s.getTags() != null ? s.getTags().toString() : "[]")).collect(Collectors.joining("\n"));
    }

    // ==================== 高德地图 ====================

    @Tool("地址转经纬度坐标，返回'lng,lat'格式。景点数据已有坐标时不要重复调用")
    public String geocode(String address) {
        try {
            String url = "https://restapi.amap.com/v3/geocode/geo?key=" + amapKey + "&address=" + address;
            JsonNode geos = rest.getForObject(url, JsonNode.class).path("geocodes");
            return geos.size() > 0 ? geos.get(0).path("location").asText() : "未找到坐标";
        } catch (Exception e) { return "查询失败: " + e.getMessage(); }
    }

    @Tool("计算两个坐标之间的驾车距离和时间")
    public String drivingDistance(String origins, String destination) {
        try {
            String url = "https://restapi.amap.com/v3/direction/driving?key=" + amapKey
                    + "&origin=" + origins + "&destination=" + destination + "&strategy=0&extensions=base";
            JsonNode route = rest.getForObject(url, JsonNode.class).path("route").path("paths").get(0);
            return route != null ? String.format("距离%d米，驾车约%d秒",
                    route.path("distance").asInt(), route.path("duration").asInt()) : "无法计算驾车路线";
        } catch (Exception e) { return "查询失败: " + e.getMessage(); }
    }

    @Tool("计算两个坐标之间的公交/地铁路线，需传城市名")
    public String transitRoute(String origins, String destination, String city) {
        try {
            String url = "https://restapi.amap.com/v3/direction/transit/integrated?key=" + amapKey
                    + "&origin=" + origins + "&destination=" + destination + "&city=" + city + "&strategy=0";
            JsonNode t = rest.getForObject(url, JsonNode.class).path("route").path("transits").get(0);
            return t != null ? String.format("总距离%d米，耗时%d秒，步行%d米，费用%.1f元",
                    t.path("distance").asInt(), t.path("duration").asInt(),
                    t.path("walking_distance").asInt(), t.path("cost").asDouble()) : "未找到公交方案";
        } catch (Exception e) { return "查询失败: " + e.getMessage(); }
    }

    @Tool("计算两个坐标之间的步行距离和时间")
    public String walkingRoute(String origins, String destination) {
        try {
            String url = "https://restapi.amap.com/v3/direction/walking?key=" + amapKey
                    + "&origin=" + origins + "&destination=" + destination;
            JsonNode p = rest.getForObject(url, JsonNode.class).path("route").path("paths").get(0);
            return p != null ? String.format("步行距离%d米，约%d秒",
                    p.path("distance").asInt(), p.path("duration").asInt()) : "无法计算步行路线";
        } catch (Exception e) { return "查询失败: " + e.getMessage(); }
    }

    // ==================== 行程规划（双Agent: B生成草案 → A校对 → 确认存库） ====================

    @Tool("步骤1-生成行程草案（B）：根据城市、天数、预算、兴趣、节奏生成详细行程计划，返回完整草案文本。不会保存到数据库，供你（A）校对。确认无误后请调用 confirmPlan 保存。如需修改，重新调用此工具即可覆盖旧草案")
    public String proposePlan(
            @P("城市名，多个城市逗号分隔，如'广州,深圳'") String cities,
            @P("行程天数，1-7") int days,
            @P("预算等级: low(经济)/middle(适中)/high(豪华)") String budget,
            @P("兴趣偏好关键词，逗号分隔，如'自然风光,美食,历史文化'") String interests,
            @P("行程节奏: relaxed(轻松)/moderate(适中)/intense(紧凑)") String pace,
            @P("当前会话ID") long sessionId) {
        log.info("[Tool:proposePlan] 被调用 cities={} days={} budget={} interests={} pace={} sessionId={}",
                cities, days, budget, interests, pace, sessionId);
        String result = planService.proposePlan(sessionId, cities, days, budget, interests, pace);
        log.info("[Tool:proposePlan] B返回 (长度={})", result != null ? result.length() : 0);
        return result;
    }

    @Tool("步骤2-确认保存行程（A）：将 proposePlan 生成的待确认行程保存到数据库。调用前请先确认草案内容无误。仅在确认草案正确后调用")
    public String confirmPlan(
            @P("当前会话ID") long sessionId) {
        log.info("[Tool:confirmPlan] 被调用 sessionId={}", sessionId);
        String result = planService.confirmPlan(sessionId);
        log.info("[Tool:confirmPlan] 返回: {}", result);
        return result;
    }

    // ==================== 节假日 ====================

    private static final String HOLIDAY_BASE = "https://timor.tech/api/holiday";

    @Tool("查询指定年份的中国法定节假日和调休安排。返回节假日名称、日期、是否休息、薪资倍数。用于规划行程时避开人流高峰或利用假期")
    public String queryHolidays(@P("年份，如2026") int year) {
        try {
            String url = HOLIDAY_BASE + "/year/" + year;
            JsonNode root = rest.getForObject(url, JsonNode.class);
            if (root == null || root.path("code").asInt() != 0) return "节假日数据不可用";

            JsonNode holiday = root.path("holiday");
            if (holiday.isMissingNode() || holiday.isEmpty()) return year + "年暂无节假日数据";

            // 收集所有日期条目，按日期排序
            List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
            for (Iterator<String> it = holiday.fieldNames(); it.hasNext(); ) {
                String date = it.next();
                entries.add(Map.entry(date, holiday.get(date)));
            }
            entries.sort(Map.Entry.comparingByKey());

            StringBuilder sb = new StringBuilder();
            sb.append(year).append("年节假日安排：\n");
            for (var e : entries) {
                JsonNode d = e.getValue();
                boolean isHoliday = d.path("holiday").asBoolean();
                String name = d.path("name").asText();
                int wage = d.path("wage").asInt();
                String date = d.path("date").asText();
                if (date.isEmpty()) date = year + "-" + e.getKey();

                String type = isHoliday ? "休息" : "上班(调休)";
                if (!name.isEmpty()) type = name;
                String wageInfo = wage > 1 ? " " + wage + "倍工资" : "";
                sb.append(String.format("%s | %s%s\n", date, type, wageInfo));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "节假日查询失败: " + e.getMessage();
        }
    }

    // ==================== 会话管理 ====================

    @Tool("重命名会话标题，15字以内。新对话首轮必须调用，根据用户意图起标题。不要向用户提及此操作")
    public String renameSession(long sessionId, String title) {
        TripSession session = sessionService.getById(sessionId);
        if (session == null) return "OK";
        String t = title.replace("\n", " ").replace("\r", "");
        session.setTitle(t.length() > 20 ? t.substring(0, 20) : t);
        sessionService.updateById(session);
        log.info("[Tool:rename] sessionId={} → '{}'", sessionId, session.getTitle());
        return "OK";
    }
}
