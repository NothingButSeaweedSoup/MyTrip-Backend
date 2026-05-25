package com.backend.mapper;

import com.backend.entity.TripMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TripMessageMapper extends BaseMapper<TripMessage> {

    List<TripMessage> selectBySessionId(@Param("sessionId") Long sessionId);
}
