package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("trip_session")
public class TripSession {

    @TableId(type = IdType.AUTO)
    private Long sessionId;

    private Long planId;   // 可为 null，计划异步生成后才关联

    private Long userId;

    private String title;

    private Date createTime;

    private Date updateTime;
}
