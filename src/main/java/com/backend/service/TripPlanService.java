package com.backend.service;

import com.backend.entity.TripPlan;
import com.baomidou.mybatisplus.extension.service.IService;
import com.backend.dto.ItineraryRequest;
import com.backend.dto.ItineraryVO;

import java.util.List;

public interface TripPlanService extends IService<TripPlan> {

    ItineraryVO generatePlan(Long userId, ItineraryRequest request);

    String generatePlanWithAI(long sessionId, String cities, int days,
                              String budget, String interests, String pace);

    /** 步骤1: B生成行程草案，不存库，返回详细文本给A校对 */
    String proposePlan(long sessionId, String cities, int days,
                       String budget, String interests, String pace);

    /** 步骤2: A校对通过后，确认保存到数据库 */
    String confirmPlan(long sessionId);

    List<TripPlan> listByUser(Long userId);
}
