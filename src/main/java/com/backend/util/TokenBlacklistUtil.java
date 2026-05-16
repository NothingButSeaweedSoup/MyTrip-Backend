package com.backend.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class TokenBlacklistUtil {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将 token 加入黑名单，过期时间对应该 token 的剩余有效期
     */
    public void blacklist(String token, Date expiration) {
        long ttl = expiration.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            stringRedisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + token, "1", ttl, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
