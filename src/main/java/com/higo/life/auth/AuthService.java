package com.higo.life.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.higo.life.cache.CacheKeys;
import com.higo.life.support.ConflictException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration CODE_RATE_TTL = Duration.ofSeconds(60);
    static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();
    private final boolean exposeCode;

    public AuthService(
            StringRedisTemplate redis,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            @Value("${higo.auth.expose-code:true}") boolean exposeCode
    ) {
        this.redis = redis;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.exposeCode = exposeCode;
    }

    public VerificationCodeResponse sendCode(String phone) {
        Boolean accepted = redis.opsForValue().setIfAbsent(
                CacheKeys.LOGIN_CODE_RATE + phone, "1", CODE_RATE_TTL
        );
        if (!Boolean.TRUE.equals(accepted)) {
            throw new ConflictException("验证码发送过于频繁，请稍后重试");
        }
        String code = "%06d".formatted(random.nextInt(1_000_000));
        redis.opsForValue().set(CacheKeys.LOGIN_CODE + phone, code, CODE_TTL);
        return new VerificationCodeResponse("验证码已生成", exposeCode ? code : null);
    }

    @Transactional
    public SessionResponse login(LoginRequest request) {
        var script = new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                "if redis.call('GET', KEYS[1]) == ARGV[1] then redis.call('DEL', KEYS[1]); return 1 else return 0 end", Long.class);
        Long consumed = redis.execute(script, java.util.List.of(CacheKeys.LOGIN_CODE + request.phone()), request.code());
        if (!Long.valueOf(1).equals(consumed)) {
            throw new ConflictException("验证码错误或已过期");
        }
        User user = userRepository.findByPhone(request.phone())
                .orElseGet(() -> userRepository.save(new User(
                        request.phone(), "嗨友_" + request.phone().substring(7)
                )));
        AuthenticatedUser authenticatedUser = AuthenticatedUser.from(user);
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(
                CacheKeys.SESSION + token, write(authenticatedUser), SESSION_TTL
        );
        return new SessionResponse(token, authenticatedUser);
    }

    public void logout(String token) {
        redis.delete(CacheKeys.SESSION + token);
    }

    String write(AuthenticatedUser user) {
        try {
            return objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法保存登录会话", exception);
        }
    }
}
