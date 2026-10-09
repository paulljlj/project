package com.higo.life.seckill;

import com.higo.life.ratelimit.RateLimit;
import com.higo.life.ratelimit.RateLimitDimension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
@ConditionalOnProperty(
        name = {"higo.redis.enabled", "higo.kafka.enabled"},
        havingValue = "true",
        matchIfMissing = true
)
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/vouchers/{voucherId}/orders")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @RateLimit(windowSeconds = 10, permits = 5, dimension = RateLimitDimension.USER)
    public SeckillReservationResponse reserve(@PathVariable Long voucherId) {
        return seckillService.reserve(voucherId);
    }
}
