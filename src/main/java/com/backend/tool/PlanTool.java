package com.backend.tool;

import com.backend.service.TripPlanService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PlanTool {

    @Autowired
    private TripPlanService planService;

    @Tool("为用户生成旅游行程计划。参数: city=城市, days=天数, budget=预算(low/middle/high), interests=兴趣标签逗号分隔, pace=节奏(relaxed/moderate/intense), sessionId=当前会话ID。返回生成结果摘要。")
    public String generatePlan(String city, int days, String budget,
                                String interests, String pace, long sessionId) {
        return planService.generatePlanWithAI(
                sessionId, city, days, budget, interests, pace);
    }
}
