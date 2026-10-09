package com.higo.life.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.higo.life.cache.CacheKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    public static final String USER_ATTRIBUTE = AuthenticatedUser.class.getName();

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SessionInterceptor(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = bearerToken(request);
        if (token == null) {
            return true;
        }
        String key = CacheKeys.SESSION + token;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return true;
        }
        try {
            request.setAttribute(USER_ATTRIBUTE, objectMapper.readValue(json, AuthenticatedUser.class));
            redis.expire(key, AuthService.SESSION_TTL);
            return true;
        } catch (JsonProcessingException exception) {
            redis.delete(key);
            return true;
        }
    }

    public static String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
