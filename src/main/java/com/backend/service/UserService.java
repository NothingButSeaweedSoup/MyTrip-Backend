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

    /** 登出，Token加入黑名单 */
    void logout(String token);

    /** 管理员：获取所有用户列表 */
    List<UserVO> listUsers(Long adminUserId);

    /** 管理员：修改用户角色 */
    void updateUserRole(Long adminUserId, Long userId, Integer role);
}
