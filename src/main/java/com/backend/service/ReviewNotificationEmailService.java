package com.backend.service;

import com.backend.entity.ReviewNotificationEmail;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ReviewNotificationEmailService extends IService<ReviewNotificationEmail> {

    /** 获取所有启用的通知邮箱 */
    List<String> listEnabledEmails();

    /** 为用户设置/更新通知邮箱 */
    void saveOrUpdateEmail(Long userId, String email);

    /** 启用/禁用某用户的通知 */
    void setEnabled(Long userId, boolean enabled);

    /** 删除某用户的通知邮箱 */
    void removeByUserId(Long userId);
}
