package com.backend.aspect;

import com.backend.annotation.RateLimit;
import com.backend.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);
        Long count = stringRedisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, rateLimit.window(), rateLimit.unit());
        }

        if (count != null && count > rateLimit.limit()) {
            return Result.error(429, "请求过于频繁，请稍后再试");
        }

        return point.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        return rateLimit.prefix() + ":" + uri + ":" + ip;
    }
}
