package com.higo.life.seckill;

import com.higo.life.auth.CurrentUser;
import com.higo.life.cache.CacheKeys;
import com.higo.life.support.ConflictException;
import com.higo.life.support.NotFoundException;
import com.higo.life.voucher.Voucher;
import com.higo.life.voucher.VoucherRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = {"higo.redis.enabled", "higo.kafka.enabled"},
        havingValue = "true",
        matchIfMissing = true
)
public class SeckillService {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> COMPENSATE_SCRIPT = new DefaultRedisScript<>();

    static {
        RESERVE_SCRIPT.setLocation(new ClassPathResource("seckill_reserve.lua"));
        RESERVE_SCRIPT.setResultType(Long.class);
        COMPENSATE_SCRIPT.setLocation(new ClassPathResource("seckill_compensate.lua"));
        COMPENSATE_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, Object> kafka;
    private final VoucherRepository voucherRepository;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final String topic;

    public SeckillService(
            StringRedisTemplate redis,
            KafkaTemplate<String, Object> kafka,
            VoucherRepository voucherRepository,
            CurrentUser currentUser,
            Clock clock,
            @Value("${higo.kafka.order-topic:higo.seckill.orders.v1}") String topic
    ) {
        this.redis = redis;
        this.kafka = kafka;
        this.voucherRepository = voucherRepository;
        this.currentUser = currentUser;
        this.clock = clock;
        this.topic = topic;
    }

    public void prepare(Voucher voucher) {
        Duration ttl = Duration.between(LocalDateTime.now(clock), voucher.getEndAt()).plusHours(1);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redis.opsForValue().set(
                CacheKeys.SECKILL_STOCK + voucher.getId(),
                Integer.toString(voucher.getStock()),
                ttl
        );
        redis.expire(CacheKeys.SECKILL_BUYERS + voucher.getId(), ttl);
    }

    public SeckillReservationResponse reserve(Long voucherId) {
        Long userId = currentUser.require().id();
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new NotFoundException("优惠券不存在: " + voucherId));
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(voucher.getBeginAt())) {
            throw new ConflictException("秒杀尚未开始");
        }
        if (now.isAfter(voucher.getEndAt())) {
            throw new ConflictException("秒杀已经结束");
        }

        List<String> keys = List.of(
                CacheKeys.SECKILL_STOCK + voucherId,
                CacheKeys.SECKILL_BUYERS + voucherId
        );
        Long result = redis.execute(RESERVE_SCRIPT, keys, userId.toString());
        if (result == null || result == 3) {
            prepare(voucher);
            result = redis.execute(RESERVE_SCRIPT, keys, userId.toString());
        }
        if (Long.valueOf(1).equals(result)) {
            throw new ConflictException("优惠券库存不足");
        }
        if (Long.valueOf(2).equals(result)) {
            throw new ConflictException("同一用户不能重复购买同一张优惠券");
        }
        if (!Long.valueOf(0).equals(result)) {
            throw new IllegalStateException("Redis 秒杀预检返回未知状态");
        }

        String requestId = UUID.randomUUID().toString();
        try {
            kafka.send(topic, voucherId.toString(), new SeckillOrderMessage(requestId, userId, voucherId))
                    .get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            redis.execute(COMPENSATE_SCRIPT, keys, userId.toString());
            throw new IllegalStateException("订单消息发送被中断，请重试", exception);
        } catch (Exception exception) {
            redis.execute(COMPENSATE_SCRIPT, keys, userId.toString());
            throw new IllegalStateException("订单消息发送失败，请重试", exception);
        }
        return new SeckillReservationResponse(requestId, "ACCEPTED");
    }
}
