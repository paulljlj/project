package com.higo.life.ratelimit;

import com.higo.life.auth.AuthenticatedUser;
import com.higo.life.auth.CurrentUser;
import com.higo.life.cache.CacheKeys;
import com.higo.life.support.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RateLimitAspect {

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>();

    static {
        SCRIPT.setLocation(new ClassPathResource("rate_limit.lua"));
        SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;
    private final CurrentUser currentUser;
    private final HttpServletRequest request;

    public RateLimitAspect(StringRedisTemplate redis, CurrentUser currentUser, HttpServletRequest request) {
        this.redis = redis;
        this.currentUser = currentUser;
        this.request = request;
    }

    @Before("@annotation(rateLimit)")
    public void check(JoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringTypeName() + ":" + signature.getName();
        String dimension = switch (rateLimit.dimension()) {
            case USER -> {
                AuthenticatedUser user = currentUser.optional();
                yield "user:" + (user == null ? "anonymous" : user.id());
            }
            case IP -> "ip:" + request.getRemoteAddr();
            case METHOD -> "method";
        };
        long now = System.currentTimeMillis();
        Long accepted = redis.execute(
                SCRIPT,
                List.of(CacheKeys.RATE_LIMIT + method + ":" + dimension),
                Long.toString(now),
                Long.toString(rateLimit.windowSeconds() * 1000),
                Long.toString(rateLimit.permits()),
                now + ":" + UUID.randomUUID()
        );
        if (!Long.valueOf(1).equals(accepted)) {
            throw new TooManyRequestsException("请求过于频繁，请稍后重试");
        }
    }
}
