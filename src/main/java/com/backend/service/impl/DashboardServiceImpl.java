package com.backend.service.impl;

import com.backend.dto.*;
import com.backend.dto.ItineraryVO;
import com.backend.entity.*;
import com.backend.mapper.UserMapper;
import com.backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private PostService postService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private TripPlanService tripPlanService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ScenicSpotService scenicSpotService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserVO enrichCurrentUser(Long userId, UserVO vo) {
        vo.setPostCount(postService.lambdaQuery().eq(Post::getAuthorId, userId)
                .ne(Post::getStatus, 4).count());
        vo.setFavoriteCount(favoriteService.lambdaQuery()
                .eq(Favorite::getUserId, userId).count());
        return vo;
    }

    @Override
    public UserDetailVO getUserDetail(Long adminUserId, Long userId) {
        User user = userMapper.selectById(userId);
        Long postCount = postService.lambdaQuery().eq(Post::getAuthorId, userId).ne(Post::getStatus, 4).count();
        Long favoriteCount = favoriteService.lambdaQuery().eq(Favorite::getUserId, userId).count();
        Long tripCount = tripPlanService.lambdaQuery().eq(TripPlan::getUserId, userId).count();
        List<Post> recentPosts = postService.lambdaQuery()
                .eq(Post::getAuthorId, userId)
                .ne(Post::getStatus, 4)
                .orderByDesc(Post::getCreateTime)
                .last("LIMIT 5")
                .list();
        List<PostVO> postVOs = recentPosts.stream().map(p -> PostVO.builder()
                .postId(p.getPostId())
                .title(p.getTitle())
                .createTime(p.getCreateTime())
                .build()).toList();
        List<TripPlan> recentTrips = tripPlanService.lambdaQuery()
                .eq(TripPlan::getUserId, userId)
                .orderByDesc(TripPlan::getCreateTime)
                .last("LIMIT 5")
                .list();
        List<ItineraryVO> tripVOs = recentTrips.stream().map(t -> ItineraryVO.builder()
                .planId(t.getPlanId())
                .title(t.getTitle())
                .days(t.getDays())
                .budget(t.getBudget())
                .build()).toList();
        return UserDetailVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .postCount(postCount.intValue())
                .favoriteCount(favoriteCount.intValue())
                .tripCount(tripCount.intValue())
                .recentPosts(postVOs)
                .recentTrips(tripVOs)
                .build();
    }

    @Override
    public DashboardVO getDashboard(Long adminUserId) {
        Long totalUsers = userMapper.selectCount(null);
        Long totalPosts = (long) postService.lambdaQuery().ne(Post::getStatus, 4).count();
        Long totalScenicSpots = (long) scenicSpotService.count();
        Long totalComments = (long) commentService.count();
        LocalDate today = LocalDate.now();
        Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Long todayNewUsers = (long) userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .ge(User::getCreateTime, todayStart));
        Long todayNewPosts = (long) postService.lambdaQuery().ge(Post::getCreateTime, todayStart).count();
        Long pendingReviewPosts = (long) postService.lambdaQuery().eq(Post::getStatus, 0).count();
        return DashboardVO.builder()
                .totalUsers(totalUsers)
                .totalPosts(totalPosts)
                .totalScenicSpots(totalScenicSpots)
                .totalComments(totalComments)
                .todayNewUsers(todayNewUsers)
                .todayNewPosts(todayNewPosts)
                .pendingReviewPosts(pendingReviewPosts)
                .pendingReviewComments(0L)
                .build();
    }
}
