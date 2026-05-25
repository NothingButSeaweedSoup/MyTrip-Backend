package com.backend.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WeatherTool {

    private final RestTemplate rest;
    private final ObjectMapper mapper;

    private static final String BASE = "https://api.open-meteo.com/v1";

    private static final String[] WMO_CODES = {
        "晴", "晴间多云", "多云", "阴",
        "雾", "毛毛雨", "小雨", "中雨", "大雨", "暴雨",
        "雨夹雪", "小雪", "中雪", "大雪", "暴雪",
        "扬沙", "沙尘暴", "雾凇", "霾"
    };

    public WeatherTool() {
        this.rest = new RestTemplate();
        this.mapper = new ObjectMapper();
    }

    @Tool("获取指定经纬度未来几天的天气，参数: lat=纬度, lng=经度, days=天数(1-7)，返回每天的温度、天气、降水概率")
    public String getDailyForecast(double lat, double lng, int days) {
        try {
            if (days < 1) days = 1;
            if (days > 7) days = 7;

            String url = BASE + "/forecast"
                    + "?latitude=" + lat + "&longitude=" + lng
                    + "&daily=temperature_2m_max,temperature_2m_min,weathercode,precipitation_probability_max"
                    + "&timezone=Asia%2FShanghai"
                    + "&forecast_days=" + days;

            JsonNode root = rest.getForObject(url, JsonNode.class);
            JsonNode daily = root.path("daily");
            if (daily.isMissingNode()) return "天气数据不可用";

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < days; i++) {
                String date = daily.path("time").get(i).asText();
                double tMax = daily.path("temperature_2m_max").get(i).asDouble();
                double tMin = daily.path("temperature_2m_min").get(i).asDouble();
                int code = daily.path("weathercode").get(i).asInt();
                int precip = daily.path("precipitation_probability_max").get(i).asInt();

                String desc = code < WMO_CODES.length ? WMO_CODES[code] : "未知";
                sb.append(String.format("%s: %s, %.0f~%.0f℃, 降水概率%d%%\n",
                        date, desc, tMin, tMax, precip));
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "天气查询失败: " + e.getMessage();
        }
    }
}
