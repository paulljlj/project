package com.higo.life.signin;

import com.higo.life.auth.CurrentUser;
import com.higo.life.cache.CacheKeys;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SignInService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final StringRedisTemplate redis;
    private final CurrentUser currentUser;
    private final Clock clock;

    public SignInService(StringRedisTemplate redis, CurrentUser currentUser, Clock clock) {
        this.redis = redis;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public void signIn() {
        Long userId = currentUser.require().id();
        LocalDate today = LocalDate.now(clock);
        redis.opsForValue().setBit(key(userId, today), today.getDayOfMonth() - 1, true);
    }

    public int consecutiveDays() {
        Long userId = currentUser.require().id();
        LocalDate today = LocalDate.now(clock);
        int day = today.getDayOfMonth();
        List<Long> values = redis.opsForValue().bitField(
                key(userId, today),
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(day))
                        .valueAt(0)
        );
        if (values == null || values.isEmpty() || values.getFirst() == null) {
            return 0;
        }
        long bits = values.getFirst();
        int count = 0;
        while ((bits & 1) == 1) {
            count++;
            bits >>>= 1;
        }
        return count;
    }

    private String key(Long userId, LocalDate date) {
        return CacheKeys.SIGN_IN + userId + ":" + date.format(MONTH);
    }
}
