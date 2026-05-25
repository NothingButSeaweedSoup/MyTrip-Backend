package com.backend.service;

import com.backend.entity.TripSession;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TripSessionService extends IService<TripSession> {

    List<TripSession> listByUser(Long userId);
}
