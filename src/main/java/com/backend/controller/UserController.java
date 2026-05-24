package com.backend.controller;

import com.backend.annotation.RateLimit;
import com.backend.common.Result;
import com.backend.dto.*;
import com.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = userService.register(request);
        return Result.success(userId);
    }

    @RateLimit(limit = 5, window = 60, prefix = "login")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return Result.success(response);
    }

    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.success(userService.getCurrentUser(userId));
    }

    @PutMapping("/preference")
    public Result<Void> updatePreference(@Valid @RequestBody PreferenceRequest request,
                                         Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        userService.updatePreference(userId, request);
        return Result.success();
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                       Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        userService.updateProfile(userId, request);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        userService.logout(token);
        return Result.success();
    }
}
