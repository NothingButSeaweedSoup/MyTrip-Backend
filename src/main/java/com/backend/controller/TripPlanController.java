package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.ItineraryRequest;
import com.backend.dto.ItineraryVO;
import com.backend.entity.TripPlan;
import com.backend.service.TripPlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trip-plan")
public class TripPlanController {

    @Autowired
    private TripPlanService tripPlanService;

    @PostMapping("/generate")
    public Result<ItineraryVO> generate(
            @Valid @RequestBody ItineraryRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        ItineraryVO vo = tripPlanService.generatePlan(userId, request);
        return Result.success(vo);
    }

    @GetMapping("/{id}")
    public Result<TripPlan> getById(@PathVariable Long id) {
        TripPlan plan = tripPlanService.getById(id);
        return plan != null ? Result.success(plan) : Result.error("行程不存在");
    }

    @GetMapping("/my")
    public Result<List<TripPlan>> myPlans(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        List<TripPlan> plans = tripPlanService.listByUser(userId);
        return Result.success(plans);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        TripPlan plan = tripPlanService.getById(id);
        if (plan == null) return Result.error("行程不存在");
        if (!plan.getUserId().equals(userId)) return Result.error("无权删除");
        tripPlanService.removeById(id);
        return Result.success();
    }
}
