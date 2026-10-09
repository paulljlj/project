package com.higo.life.seckill;

public record SeckillOrderMessage(String requestId, Long userId, Long voucherId) {
}
