package com.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardVO {
    private Long totalUsers;
    private Long totalPosts;
    private Long totalScenicSpots;
    private Long totalComments;
    private Long todayNewUsers;
    private Long todayNewPosts;
    private Long pendingReviewPosts;
    private Long pendingReviewComments;
}
