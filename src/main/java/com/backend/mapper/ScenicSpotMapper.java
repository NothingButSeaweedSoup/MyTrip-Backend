package com.backend.mapper;

import com.backend.entity.ScenicSpot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author Administrator
* @description 针对表【scenic_spot(景点表)】的数据库操作Mapper
* @createDate 2026-05-16 00:16:36
* @Entity com.backend.entity.ScenicSpot
*/
public interface ScenicSpotMapper extends BaseMapper<ScenicSpot> {

    java.util.List<ScenicSpot> selectByCity(@Param("city") String city);
}




