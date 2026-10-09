package com.higo.life.shop;

import com.higo.life.cache.CacheKeys;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "higo.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ShopGeoService {

    private final StringRedisTemplate redis;

    public ShopGeoService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void index(Shop shop) {
        if (shop.getLongitude() == null || shop.getLatitude() == null) {
            return;
        }
        redis.opsForGeo().add(
                CacheKeys.SHOP_GEO + shop.getCategory(),
                new Point(shop.getLongitude(), shop.getLatitude()),
                shop.getId().toString()
        );
    }

    public void remove(String category, Long shopId) {
        redis.opsForZSet().remove(CacheKeys.SHOP_GEO + category, shopId.toString());
    }

    public Map<Long, Double> nearby(String category, double longitude, double latitude, int limit) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo().search(
                CacheKeys.SHOP_GEO + category,
                GeoReference.fromCoordinate(longitude, latitude),
                new Distance(5, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().sortAscending().limit(limit)
        );
        if (results == null) {
            return Map.of();
        }
        Map<Long, Double> distances = new LinkedHashMap<>();
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            Distance distance = result.getDistance();
            distances.put(
                    Long.valueOf(result.getContent().getName()),
                    distance == null ? null : distance.getValue() * 1000
            );
        }
        return distances;
    }
}
