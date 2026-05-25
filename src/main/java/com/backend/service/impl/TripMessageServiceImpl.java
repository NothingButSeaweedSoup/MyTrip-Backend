package com.backend.service.impl;

import com.backend.entity.TripMessage;
import com.backend.mapper.TripMessageMapper;
import com.backend.service.TripMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TripMessageServiceImpl
        extends ServiceImpl<TripMessageMapper, TripMessage>
        implements TripMessageService {

    @Autowired
    private TripMessageMapper messageMapper;

    @Override
    public List<TripMessage> listBySession(Long sessionId) {
        return messageMapper.selectBySessionId(sessionId);
    }
}
