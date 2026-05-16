package com.backend.service.impl;

import com.backend.common.BusinessException;
import com.backend.common.UnauthorizedException;
import com.backend.dto.*;
import com.backend.entity.User;
import com.backend.mapper.UserMapper;
import com.backend.service.UserService;
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
        return toUserVO(user);
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
                .createTime(user.getCreateTime())
                .build();
    }
}
