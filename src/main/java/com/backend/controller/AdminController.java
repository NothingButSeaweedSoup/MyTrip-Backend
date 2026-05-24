package com.backend.controller;

import com.backend.common.Result;
import com.backend.dto.UpdateRoleRequest;
import com.backend.dto.UserVO;
import com.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    @Autowired
    private UserService userService;

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
}
