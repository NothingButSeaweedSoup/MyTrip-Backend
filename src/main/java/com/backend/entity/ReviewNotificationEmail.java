package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName("review_notification_email")
@Data
public class ReviewNotificationEmail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String email;

    private Integer enabled;

    private Date createTime;
}
