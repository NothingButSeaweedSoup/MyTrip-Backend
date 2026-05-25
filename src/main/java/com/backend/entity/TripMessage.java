package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("trip_message")
public class TripMessage {

    @TableId(type = IdType.AUTO)
    private Long messageId;

    private Long sessionId;

    private String role;       // user / ai / tool

    private String content;

    private String toolName;

    private String toolResult;

    private Date createTime;
}
