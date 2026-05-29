package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.*;
import com.backend.service.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ScenicSpotService scenicSpotService;

    @Autowired
    private TagService tagService;

    @Autowired
    private AiReviewService aiReviewService;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private PostAuditRecordService postAuditRecordService;

    // ========== 用户管理 ==========

    @GetMapping("/users")
    public Result<List<UserVO>> listUsers(Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        return Result.success(userService.listUsers(adminUserId));
    }

    @PutMapping("/users/{userId}/role")
    public Result<Void> updateUserRole(@PathVariable Long userId,
                                       @Valid @RequestBody UpdateRoleRequest request,
                                       Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        userService.updateUserRole(adminUserId, userId, request.getRole());
        return Result.success();
    }

    @PutMapping("/users/{userId}/status")
    public Result<Void> updateUserStatus(@PathVariable Long userId,
                                         @Valid @RequestBody UpdateUserStatusRequest request,
                                         Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        userService.updateUserStatus(adminUserId, userId, request.getStatus());
        return Result.success();
    }

    // ========== 景点管理 ==========

    @PostMapping("/scenic-spots")
    public Result<Long> createScenicSpot(@Valid @RequestBody ScenicSpotCreateRequest request,
                                         Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        return Result.success(scenicSpotService.createSpot(adminUserId, request));
    }

    @PutMapping("/scenic-spots/{spotId}")
    public Result<Void> updateScenicSpot(@PathVariable Long spotId,
                                         @Valid @RequestBody ScenicSpotEditRequest request,
                                         Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        scenicSpotService.updateSpot(adminUserId, spotId, request);
        return Result.success();
    }

    @GetMapping("/scenic-spots")
    public Result<IPage<ScenicSpotVO>> listScenicSpots(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        return Result.success(scenicSpotService.listSpotsForAdmin(adminUserId, city, keyword, page, pageSize));
    }

    @PutMapping("/scenic-spots/{spotId}/status")
    public Result<Void> updateScenicSpotStatus(@PathVariable Long spotId,
                                               @Valid @RequestBody UpdateUserStatusRequest request,
                                               Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        scenicSpotService.updateSpotStatus(adminUserId, spotId, request.getStatus());
        return Result.success();
    }

    // ========== 标签管理 ==========

    @PostMapping("/tags")
    public Result<Integer> createTag(@Valid @RequestBody TagCreateRequest request,
                                     Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        return Result.success(tagService.createTag(adminUserId, request));
    }

    @PutMapping("/tags/{tagId}")
    public Result<Void> updateTag(@PathVariable Integer tagId,
                                  @Valid @RequestBody TagEditRequest request,
                                  Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        tagService.updateTag(adminUserId, tagId, request);
        return Result.success();
    }

    @GetMapping("/tags")
    public Result<IPage<TagVO>> listTags(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        return Result.success(tagService.listTagsForAdmin(adminUserId, keyword, page, pageSize));
    }

    @DeleteMapping("/tags/{tagId}")
    public Result<Void> deleteTag(@PathVariable Integer tagId, Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        tagService.deleteTag(adminUserId, tagId);
        return Result.success();
    }

    // ========== AI审核配置 ==========

    @GetMapping("/ai-prompts")
    public Result<AiPromptVO> getAiPrompts(Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        userService.checkAdminRole(adminUserId);
        return Result.success(aiReviewService.getPromptConfig());
    }

    @PutMapping("/ai-prompts")
    public Result<Void> updateAiPrompts(@Valid @RequestBody AiPromptUpdateRequest request,
                                        Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        userService.checkAdminRole(adminUserId);
        aiReviewService.updatePrompt(request.getPrompt());
        return Result.success();
    }

    // ========== AI推荐配置 ==========

    @GetMapping("/recommend-config")
    public Result<RecommendConfigVO> getRecommendConfig(Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        userService.checkAdminRole(adminUserId);
        return Result.success(recommendService.getConfig());
    }

    @PutMapping("/recommend-config")
    public Result<Void> updateRecommendConfig(@Valid @RequestBody RecommendConfigUpdateRequest request,
                                              Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        recommendService.updateConfig(adminUserId, request);
        return Result.success();
    }

    // ========== 景点批量导入 ==========

    @PostMapping("/scenic-spots/import")
    public Result<String> importScenicSpots(@RequestParam("file") MultipartFile file,
                                            Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        scenicSpotService.importSpots(adminUserId, file);
        return Result.success("导入成功");
    }

    // ========== 审核历史 ==========

    @GetMapping("/audit-history")
    public Result<IPage<AuditRecordVO>> getAuditHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String targetType,
            Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        return Result.success(postAuditRecordService.getAuditHistory(adminUserId, page, pageSize, targetType));
    }

    // ========== 用户详情 ==========

    @GetMapping("/users/{userId}/detail")
    public Result<UserDetailVO> getUserDetail(@PathVariable Long userId, Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        return Result.success(userService.getUserDetail(adminUserId, userId));
    }

    // ========== 仪表盘 ==========

    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard(Authentication auth) {
        Long adminUserId = (Long) auth.getPrincipal();
        return Result.success(userService.getDashboard(adminUserId));
    }
}
