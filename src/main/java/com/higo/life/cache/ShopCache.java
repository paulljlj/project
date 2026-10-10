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
    private final org.springframework.core.task.TaskExecutor executor;
    private final com.higo.life.shop.ShopRepository shops;

    public ShopCache(StringRedisTemplate redis, ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Qualifier("cacheRebuildExecutor") org.springframework.core.task.TaskExecutor executor,
            com.higo.life.shop.ShopRepository shops) {
        this.redis = redis;
        this.objectMapper = objectMapper; this.executor=executor; this.shops=shops;
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

    public record LogicalEntry(ShopResponse value, long expiresAt) {}
    public Optional<ShopResponse> logical(Long id, Supplier<Optional<ShopResponse>> lookup) {
        String key=CacheKeys.SHOP+"hot:"+id;
        String json=redis.opsForValue().get(key);
        if(json==null) { var loaded=lookup.get(); loaded.ifPresent(v->saveLogical(key,v)); return loaded; }
        try {
            var entry=objectMapper.readValue(json,LogicalEntry.class);
            if(entry.expiresAt()<System.currentTimeMillis()) {
                String lock=key+":lock", token=java.util.UUID.randomUUID().toString();
                if(Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lock,token,Duration.ofSeconds(15)))) {
                    try { executor.execute(()-> { try {
                        var loaded=shops.findById(id).map(ShopResponse::from);
                        // Rebuild only while this task still owns the lease. Never resurrect an evicted entry.
                        if(token.equals(redis.opsForValue().get(lock)) && Boolean.TRUE.equals(redis.hasKey(key))) {
                            loaded.ifPresentOrElse(v->replaceLogical(key,json,v),()->redis.execute(
                                new org.springframework.data.redis.core.script.DefaultRedisScript<Long>("if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) else return 0 end",Long.class),java.util.List.of(key),json));
                        }
                    } finally { unlock(lock,token); } }); }
                    catch(org.springframework.core.task.TaskRejectedException ex) { unlock(lock,token); }
                }
            }
            return Optional.of(entry.value());
        } catch(JsonProcessingException ex) { redis.delete(key); return lookup.get(); }
    }
    private void replaceLogical(String key,String expected,ShopResponse value) {
        try {
            String replacement=objectMapper.writeValueAsString(new LogicalEntry(value,System.currentTimeMillis()+30_000));
            redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                    "if redis.call('GET',KEYS[1])==ARGV[1] then redis.call('SET',KEYS[1],ARGV[2],'EX',3600);return 1 else return 0 end",Long.class),java.util.List.of(key),expected,replacement);
        } catch(JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }
    private void saveLogical(String key,ShopResponse value) {
        try { redis.opsForValue().set(key,objectMapper.writeValueAsString(new LogicalEntry(value,System.currentTimeMillis()+30_000)),Duration.ofHours(1)); }
        catch(JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }
    private void unlock(String key,String token) {
        redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                "if redis.call('GET',KEYS[1])==ARGV[1] then return redis.call('DEL',KEYS[1]) else return 0 end",Long.class), java.util.List.of(key),token);
    }
    public void evict(Long id) {
        redis.delete(java.util.List.of(CacheKeys.SHOP + id, CacheKeys.SHOP + "hot:" + id));
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
