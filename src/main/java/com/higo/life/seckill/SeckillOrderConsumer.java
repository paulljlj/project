package com.higo.life.seckill;

import com.higo.life.order.OrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "higo.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class SeckillOrderConsumer {

    private final OrderService orderService;

    public SeckillOrderConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${higo.kafka.order-topic:higo.seckill.orders.v1}")
    public void consume(SeckillOrderMessage message) {
        orderService.placeReserved(message.requestId(), message.userId(), message.voucherId());
    }
}
