package com.backend.service;

import com.backend.dto.*;
import com.backend.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserService extends IService<User> {

    /** 注册，返回用户ID */
    Long register(RegisterRequest request);

    /** 邮箱+密码登录，返回JWT和用户信息 */
    LoginResponse login(LoginRequest request);

    /** 获取当前用户信息 */
    UserVO getCurrentUser(Long userId);

    /** 更新用户偏好 */
    void updatePreference(Long userId, PreferenceRequest request);

    /** 更新个人资料 */
    void updateProfile(Long userId, ProfileUpdateRequest request);

    /** 修改密码 */
    void changePassword(Long userId, ChangePasswordRequest request);

    /** 登出，Token加入黑名单 */
    void logout(String token);

    /** 管理员：获取所有用户列表 */
    List<UserVO> listUsers(Long adminUserId);

    /** 管理员：修改用户角色 */
    void updateUserRole(Long adminUserId, Long userId, Integer role);

    /** 管理员：封禁/解封用户 */
    void updateUserStatus(Long adminUserId, Long userId, Integer status);

    /** 检查用户是否为管理员，不是则抛出异常 */
    void checkAdminRole(Long userId);

    /** 管理员：获取用户详情（含发帖、收藏、行程） */
    UserDetailVO getUserDetail(Long adminUserId, Long userId);

    /** 管理员：获取仪表盘数据 */
    DashboardVO getDashboard(Long adminUserId);
}
