package com.backend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 时间窗口内允许的最大请求数 */
    int limit() default 5;

    /** 时间窗口长度 */
    long window() default 60;

    /** 时间单位 */
    TimeUnit unit() default TimeUnit.SECONDS;

    /** Redis key 前缀 */
    String prefix() default "rate_limit";
}
