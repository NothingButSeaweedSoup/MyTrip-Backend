package com.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class UserDetailVO {
    private Long userId;
    private String username;
    private String email;
    private String avatar;
    private String bio;
    private Integer role;
    private Integer status;
    private Date createTime;
    private Integer postCount;
    private Integer favoriteCount;
    private Integer tripCount;
    private List<PostVO> recentPosts;
    private List<ItineraryVO> recentTrips;
}
