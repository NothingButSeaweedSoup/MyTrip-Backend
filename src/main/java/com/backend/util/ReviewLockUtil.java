package com.backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class ReviewLockUtil {

    @Value("${review.lock.timeout-minutes:5}")
    private long lockTimeoutMinutes;

    private final StringRedisTemplate stringRedisTemplate;

    private static final String PREFIX = "review:lock:";

    public ReviewLockUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean tryAcquire(Long postId, Long userId) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue()
                        .setIfAbsent(PREFIX + postId, String.valueOf(userId),
                                lockTimeoutMinutes, TimeUnit.MINUTES)
        );
    }

    /** 续期：仅当锁属于同一用户时刷新 TTL，返回 true 表示续期成功 */
    public boolean renew(Long postId, Long userId) {
        String key = PREFIX + postId;
        String owner = stringRedisTemplate.opsForValue().get(key);
        if (owner == null) {
            return false;
        }
        if (!owner.equals(String.valueOf(userId))) {
            return false;
        }
        stringRedisTemplate.expire(key, lockTimeoutMinutes, TimeUnit.MINUTES);
        return true;
    }

    public void release(Long postId) {
        stringRedisTemplate.delete(PREFIX + postId);
    }

    public boolean isLocked(Long postId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(PREFIX + postId));
    }

    public Set<Long> getLockedPostIds() {
        Set<String> keys = stringRedisTemplate.keys(PREFIX + "*");
        if (keys == null || keys.isEmpty()) return Collections.emptySet();
        return keys.stream()
                .map(k -> Long.parseLong(k.substring(PREFIX.length())))
                .collect(Collectors.toSet());
    }
}
