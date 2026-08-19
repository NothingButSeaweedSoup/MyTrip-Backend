package com.backend.task;

import com.backend.common.RedisKeys;
import com.backend.entity.UserBehavior;
import com.backend.mapper.PostMapper;
import com.backend.mapper.UserBehaviorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ViewCountTask {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Redis key constants → com.backend.common.RedisKeys

    @Async
    public void incrementViews(Long postId, Long userId) {
        try {
            stringRedisTemplate.opsForValue().increment(RedisKeys.POST_VIEWS + postId);
            stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_POSTS, postId.toString());

            // 写入用户浏览行为记录
            if (userId != null) {
                UserBehavior ub = new UserBehavior();
                ub.setUserId(userId);
                ub.setPostId(postId);
                ub.setActionType("view");
                ub.setCreateTime(new Date());
                userBehaviorMapper.insert(ub);
            }
        } catch (Exception e) {
            postMapper.incrementViews(postId);
        }
    }
}
