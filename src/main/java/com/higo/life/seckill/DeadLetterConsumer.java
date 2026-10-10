package com.higo.life.seckill;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;
@Component @ConditionalOnProperty(name="higo.kafka.enabled",havingValue="true",matchIfMissing=true)
public class DeadLetterConsumer {
    private final OrderRequestRepository requests;
    public DeadLetterConsumer(OrderRequestRepository requests) {this.requests=requests;}
    @KafkaListener(topics="${higo.kafka.order-topic:higo.seckill.orders.v1}.DLT",groupId="higo-dead-letter-workers")
    @Transactional public void failed(SeckillOrderMessage message) {
        requests.lockById(message.requestId()).ifPresent(r->{if(!r.getStatus().equals("COMPLETED")) r.failed("消费重试耗尽，请检查库存和数据库后重试");});
    }
}
