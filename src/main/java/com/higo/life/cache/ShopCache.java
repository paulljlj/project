package com.higo.life.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.higo.life.shop.ShopResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "higo.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ShopCache {

    private static final String NULL_VALUE = "__NULL__";
    private static final Duration NULL_TTL = Duration.ofMinutes(2);
    private static final Duration BASE_TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ShopCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public Optional<ShopResponse> get(Long id, Supplier<Optional<ShopResponse>> databaseLookup) {
        String key = CacheKeys.SHOP + id;
        String cached = redis.opsForValue().get(key);
        if (NULL_VALUE.equals(cached)) {
            return Optional.empty();
        }
        if (cached != null) {
            return Optional.of(read(cached));
        }

        Optional<ShopResponse> loaded = databaseLookup.get();
        if (loaded.isEmpty()) {
            redis.opsForValue().set(key, NULL_VALUE, NULL_TTL);
            return Optional.empty();
        }

        long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 301);
        redis.opsForValue().set(key, write(loaded.get()), BASE_TTL.plusSeconds(jitterSeconds));
        return loaded;
    }

    public void evict(Long id) {
        redis.delete(CacheKeys.SHOP + id);
    }

    private String write(ShopResponse value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化商家缓存", exception);
        }
    }

    private ShopResponse read(String value) {
        try {
            return objectMapper.readValue(value, ShopResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法读取商家缓存", exception);
        }
    }
}
