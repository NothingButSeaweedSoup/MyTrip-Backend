package com.backend.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class LocationTool {

    @Tool("""
            请求用户授权GPS定位。
            当用户问"附近有什么"、"推荐周边的"、"离我最近的"等需要当前位置的场景时调用。
            返回 [LOCATION_REQUEST] 标记，前端检测到此标记后触发浏览器定位API。
            用户同意后坐标会以消息形式回传给对话。""")
    public String requestUserLocation() {
        return "[LOCATION_REQUEST] 请点击允许获取位置信息，以便为您推荐附近景点和餐厅。";
    }
}
