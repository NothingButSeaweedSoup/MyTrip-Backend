package com.backend.service.impl;

import com.backend.entity.ReviewNotificationEmail;
import com.backend.mapper.ReviewNotificationEmailMapper;
import com.backend.service.ReviewNotificationEmailService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewNotificationEmailServiceImpl
        extends ServiceImpl<ReviewNotificationEmailMapper, ReviewNotificationEmail>
        implements ReviewNotificationEmailService {

    @Override
    public List<String> listEnabledEmails() {
        return lambdaQuery()
                .eq(ReviewNotificationEmail::getEnabled, 1)
                .list()
                .stream()
                .map(ReviewNotificationEmail::getEmail)
                .collect(Collectors.toList());
    }

    @Override
    public void saveOrUpdateEmail(Long userId, String email) {
        ReviewNotificationEmail existing = lambdaQuery()
                .eq(ReviewNotificationEmail::getUserId, userId)
                .one();
        if (existing != null) {
            existing.setEmail(email);
            updateById(existing);
        } else {
            ReviewNotificationEmail entity = new ReviewNotificationEmail();
            entity.setUserId(userId);
            entity.setEmail(email);
            entity.setEnabled(1);
            save(entity);
        }
    }

    @Override
    public void setEnabled(Long userId, boolean enabled) {
        ReviewNotificationEmail entity = lambdaQuery()
                .eq(ReviewNotificationEmail::getUserId, userId)
                .one();
        if (entity != null) {
            entity.setEnabled(enabled ? 1 : 0);
            updateById(entity);
        }
    }

    @Override
    public void removeByUserId(Long userId) {
        remove(new LambdaQueryWrapper<ReviewNotificationEmail>()
                .eq(ReviewNotificationEmail::getUserId, userId));
    }
}
