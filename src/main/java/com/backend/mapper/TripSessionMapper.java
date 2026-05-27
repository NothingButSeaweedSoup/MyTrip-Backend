package com.backend.mapper;

import com.backend.entity.TripSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TripSessionMapper extends BaseMapper<TripSession> {

    List<TripSession> selectByUserId(@Param("userId") Long userId);

    Long findUserIdBySessionId(@Param("sessionId") Long sessionId);
}
