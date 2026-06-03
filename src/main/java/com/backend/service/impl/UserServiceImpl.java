package com.backend.service.impl;

import com.backend.common.BusinessException;
import com.backend.common.UnauthorizedException;
import com.backend.dto.*;
import com.backend.entity.User;
import com.backend.mapper.UserMapper;
import com.backend.service.DashboardService;
import com.backend.service.UserService;
import com.backend.util.ImageUrlUtil;
import com.backend.util.JwtUtil;
import com.backend.util.TokenBlacklistUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistUtil tokenBlacklistUtil;

    @Autowired
    private ImageUrlUtil imageUrlUtil;

    @Autowired
    private DashboardService dashboardService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Long register(RegisterRequest request) {
        if (lambdaQuery().eq(User::getEmail, request.getEmail()).exists()) {
            throw new BusinessException("该邮箱已被注册");
        }
        if (lambdaQuery().eq(User::getUsername, request.getUsername()).exists()) {
            throw new BusinessException("该用户名已被使用");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(0);
        user.setStatus(0);
        save(user);
        return user.getUserId();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = baseMapper.selectByEmail(request.getEmail());
        if (user == null) {
            throw new UnauthorizedException("邮箱或密码错误");
        }
        if (user.getStatus() == 1) {
            throw new UnauthorizedException("账户已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("邮箱或密码错误");
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .userInfo(toUserVO(user))
                .build();
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return dashboardService.enrichCurrentUser(userId, toUserVO(user));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        User update = new User();
        update.setUserId(userId);
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(update);
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = new User();
        user.setUserId(userId);
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getPreferredTags() != null) {
            try {
                user.setPreferredTags(objectMapper.writeValueAsString(request.getPreferredTags()));
            } catch (JsonProcessingException e) {
                throw new BusinessException("偏好标签格式错误");
            }
        }
        if (request.getBudgetLevel() != null) user.setBudgetLevel(request.getBudgetLevel());
        updateById(user);
    }

    @Override
    @Transactional
    public void updatePreference(Long userId, PreferenceRequest request) {
        User user = new User();
        user.setUserId(userId);
        try {
            if (request.getPreferredTags() != null) {
                user.setPreferredTags(objectMapper.writeValueAsString(request.getPreferredTags()));
            }
        } catch (JsonProcessingException e) {
            throw new BusinessException("偏好标签格式错误");
        }
        user.setBudgetLevel(request.getBudgetLevel());
        updateById(user);
    }

    @Override
    public void logout(String token) {
        tokenBlacklistUtil.blacklist(token, jwtUtil.getExpirationFromToken(token));
    }

    @Override
    public List<UserVO> listUsers(Long adminUserId) {
        checkAdminRole(adminUserId);
        List<User> users = list();
        return users.stream().map(this::toUserVO).toList();
    }

    @Override
    @Transactional
    public void updateUserRole(Long adminUserId, Long userId, Integer role) {
        checkAdminRole(adminUserId);
        if (role == null || (role != 0 && role != 1 && role != 9)) {
            throw new BusinessException("无效的角色值");
        }
        if (adminUserId.equals(userId)) {
            throw new BusinessException("不能修改自己的角色");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        User update = new User();
        update.setUserId(userId);
        update.setRole(role);
        updateById(update);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long adminUserId, Long userId, Integer status) {
        checkAdminRole(adminUserId);
        if (adminUserId.equals(userId)) {
            throw new BusinessException("不能封禁自己");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("无效的状态值");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getRole() == 9) {
            throw new BusinessException("不能封禁管理员账号");
        }
        User update = new User();
        update.setUserId(userId);
        update.setStatus(status);
        updateById(update);
    }

    @Override
    public void checkAdminRole(Long userId) {
        User user = getById(userId);
        if (user == null || user.getRole() != 9) {
            throw new UnauthorizedException("无管理员权限");
        }
    }

    @Override
    public UserDetailVO getUserDetail(Long adminUserId, Long userId) {
        checkAdminRole(adminUserId);
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return dashboardService.getUserDetail(adminUserId, userId);
    }

    @Override
    public DashboardVO getDashboard(Long adminUserId) {
        checkAdminRole(adminUserId);
        return dashboardService.getDashboard(adminUserId);
    }

    private UserVO toUserVO(User user) {
        List<Integer> tags = Collections.emptyList();
        if (user.getPreferredTags() != null) {
            try {
                tags = objectMapper.readValue(user.getPreferredTags(),
                        new TypeReference<List<Integer>>() {});
            } catch (Exception ignored) {
            }
        }
        return UserVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .bio(user.getBio())
                .preferredTags(tags)
                .budgetLevel(user.getBudgetLevel())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
    }
}
