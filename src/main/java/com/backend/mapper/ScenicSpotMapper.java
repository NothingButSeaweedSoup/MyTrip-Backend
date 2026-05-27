package com.backend.mapper;

import com.backend.entity.ScenicSpot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ScenicSpotMapper extends BaseMapper<ScenicSpot> {

    List<ScenicSpot> selectByCity(@Param("city") String city);

    /** 关键词搜索景点（名称、城市、地址、简介） */
    @Select("SELECT spot_id, name, city, address, latitude, longitude, " +
            "description, tags, rating, visit_duration, open_time, " +
            "phone, cover_image, status, create_time, update_time " +
            "FROM scenic_spot WHERE status = 0 AND " +
            "(name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR city LIKE CONCAT('%', #{keyword}, '%') " +
            "OR address LIKE CONCAT('%', #{keyword}, '%') " +
            "OR description LIKE CONCAT('%', #{keyword}, '%'))")
    List<ScenicSpot> keywordSearch(@Param("keyword") String keyword);
}
