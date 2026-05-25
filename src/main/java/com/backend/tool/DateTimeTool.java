package com.backend.tool;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeTool {

    @Tool("获取当前日期和时间，用于规划行程时确定具体日期、星期几")
    public String getCurrentDateTime() {
        ZonedDateTime now = ZonedDateTime.now(java.time.ZoneId.of("Asia/Shanghai"));
        return String.format("当前时间: %s (%s, 第%d周)",
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                now.getDayOfWeek(),
                now.get(java.time.temporal.WeekFields.ISO.weekOfYear()));
    }
}
