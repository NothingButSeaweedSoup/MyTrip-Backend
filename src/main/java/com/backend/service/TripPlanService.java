package com.backend.service;

import com.backend.entity.TripPlan;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Administrator
* @description 针对表【trip_plan(AI行程计划表)】的数据库操作Service
* @createDate 2026-05-16 00:16:36
*/
import com.backend.dto.ItineraryRequest;
import com.backend.dto.ItineraryVO;

import java.util.List;

public interface TripPlanService extends IService<TripPlan> {

    ItineraryVO generatePlan(Long userId, ItineraryRequest request);

    String generatePlanWithAI(long sessionId, String city, int days,
                              String budget, String interests, String pace);

    List<TripPlan> listByUser(Long userId);
}
