package com.backend.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AmapTool {

    private final String apiKey;
    private final RestTemplate rest;
    private final ObjectMapper mapper;

    private static final String BASE = "https://restapi.amap.com/v3";

    public AmapTool(@Value("${amap.api-key}") String apiKey) {
        this.apiKey = apiKey;
        this.rest = new RestTemplate();
        this.mapper = new ObjectMapper();
    }

    @Tool("根据地址获取经纬度坐标，返回 lat,lng")
    public String geocode(String address) {
        try {
            String url = BASE + "/geocode/geo?key=" + apiKey
                    + "&address=" + address;
            JsonNode root = rest.getForObject(url, JsonNode.class);
            JsonNode geos = root.path("geocodes");
            if (geos.size() > 0) {
                String loc = geos.get(0).path("location").asText();
                return loc; // "lng,lat"
            }
            return "未找到坐标";
        } catch (Exception e) {
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool("计算两个坐标之间的驾车距离(米)和预计时间(秒)，参数格式: lng1,lat1|lng2,lat2")
    public String drivingDistance(String origins, String destination) {
        try {
            String url = BASE + "/direction/driving?key=" + apiKey
                    + "&origin=" + origins + "&destination=" + destination
                    + "&strategy=0&extensions=base";
            JsonNode root = rest.getForObject(url, JsonNode.class);
            JsonNode route = root.path("route").path("paths").get(0);
            if (route != null) {
                int dist = route.path("distance").asInt();
                int dura = route.path("duration").asInt();
                return String.format("距离%d米，驾车约%d秒", dist, dura);
            }
            return "无法计算驾车路线";
        } catch (Exception e) {
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool("查询两个坐标之间的公交/地铁换乘方案，参数格式: lng1,lat1|lng2,lat2，city为城市名如广州")
    public String transitRoute(String origins, String destination, String city) {
        try {
            String url = BASE + "/direction/transit/integrated?key=" + apiKey
                    + "&origin=" + origins + "&destination=" + destination
                    + "&city=" + city + "&strategy=0";
            JsonNode root = rest.getForObject(url, JsonNode.class);
            JsonNode transits = root.path("route").path("transits");
            if (transits.size() > 0) {
                JsonNode best = transits.get(0);
                int dist = best.path("distance").asInt();
                int dura = best.path("duration").asInt();
                int walk = best.path("walking_distance").asInt();
                return String.format(
                        "总距离%d米，耗时%d秒，步行%d米，费用%.1f元",
                        dist, dura, walk,
                        best.path("cost").asDouble()
                );
            }
            return "未找到公交方案";
        } catch (Exception e) {
            return "查询失败: " + e.getMessage();
        }
    }

    @Tool("查询两个坐标之间的步行距离和时间，参数格式: lng1,lat1|lng2,lat2")
    public String walkingRoute(String origins, String destination) {
        try {
            String url = BASE + "/direction/walking?key=" + apiKey
                    + "&origin=" + origins + "&destination=" + destination;
            JsonNode root = rest.getForObject(url, JsonNode.class);
            JsonNode path = root.path("route").path("paths").get(0);
            if (path != null) {
                int dist = path.path("distance").asInt();
                int dura = path.path("duration").asInt();
                return String.format("步行距离%d米，约%d秒", dist, dura);
            }
            return "无法计算步行路线";
        } catch (Exception e) {
            return "查询失败: " + e.getMessage();
        }
    }
}
