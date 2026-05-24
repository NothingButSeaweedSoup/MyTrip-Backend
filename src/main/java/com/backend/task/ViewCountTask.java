package com.backend.task;

import com.backend.common.RedisKeys;
import com.backend.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ViewCountTask {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Redis key constants → com.backend.common.RedisKeys

    @Async
    public void incrementViews(Long postId) {
        try {
            stringRedisTemplate.opsForValue().increment(RedisKeys.POST_VIEWS + postId);
            stringRedisTemplate.opsForSet().add(RedisKeys.DIRTY_POSTS, postId.toString());
        } catch (Exception e) {
            postMapper.incrementViews(postId);
        }
    }
}
