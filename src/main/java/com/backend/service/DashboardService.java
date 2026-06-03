package com.backend.service;

import com.backend.dto.DashboardVO;
import com.backend.dto.UserDetailVO;
import com.backend.dto.UserVO;

public interface DashboardService {

    DashboardVO getDashboard(Long adminUserId);

    UserDetailVO getUserDetail(Long adminUserId, Long userId);

    UserVO enrichCurrentUser(Long userId, UserVO vo);
}
