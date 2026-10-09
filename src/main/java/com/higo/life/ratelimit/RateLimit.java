package com.higo.life.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    long windowSeconds() default 10;

    long permits() default 10;

    RateLimitDimension dimension() default RateLimitDimension.USER;
}
