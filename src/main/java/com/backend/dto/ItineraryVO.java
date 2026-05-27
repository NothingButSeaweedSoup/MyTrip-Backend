package com.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryVO {
    private Long planId;
    private String title;
    private int days;
    private String budget;
    private String weather;
    private List<DayPlan> itinerary;
    private List<LocationItem> locations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationItem {
        private Long locationId;
        private String name;
        private String city;
        private String address;
        private Double latitude;
        private Double longitude;
        private Integer dayNumber;
        private Integer sortOrder;
        private String timeSlot;
        private String duration;
        private String transport;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        private int day;
        private String date;
        private String weather;
        private List<SpotItem> spots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpotItem {
        private String timeSlot;
        private String name;
        private String address;
        private String duration;
        private String transport;
        private String note;
        private Double lat;
        private Double lng;
    }
}
