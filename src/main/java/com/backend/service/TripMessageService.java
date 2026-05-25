package com.backend.service;

import com.backend.entity.TripMessage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface TripMessageService extends IService<TripMessage> {

    List<TripMessage> listBySession(Long sessionId);
}
