package com.backend.service.impl;

import com.backend.entity.TripSession;
import com.backend.mapper.TripSessionMapper;
import com.backend.service.TripSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripSessionServiceImpl
        extends ServiceImpl<TripSessionMapper, TripSession>
        implements TripSessionService {

    @Autowired
    private TripSessionMapper sessionMapper;

    @Override
    public List<TripSession> listByUser(Long userId) {
        return sessionMapper.selectByUserId(userId);
    }
}
