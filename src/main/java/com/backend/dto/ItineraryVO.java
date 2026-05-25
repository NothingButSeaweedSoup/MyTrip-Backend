package com.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ItineraryVO {
    private Long planId;
    private String destination;
    private int days;
    private String budget;
    private String weather;
    private List<DayPlan> itinerary;

    @Data
    @Builder
    public static class DayPlan {
        private int day;
        private String date;
        private String weather;
        private List<SpotItem> spots;
    }

    @Data
    @Builder
    public static class SpotItem {
        private String timeSlot;     // 上午/中午/下午/晚上
        private String name;
        private String address;
        private String duration;
        private String transport;    // 从前一景点过来的交通方式
        private String note;
    }
}
