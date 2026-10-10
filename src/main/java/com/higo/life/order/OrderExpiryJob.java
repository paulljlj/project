package com.higo.life.order;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Each cancellation uses the same row lock as payment; concurrent workers are idempotent. */
@Component
public class OrderExpiryJob {
    private final VoucherOrderRepository orders;
    private final OrderService service;
    private final Clock clock;
    private final long timeoutMinutes;
    public OrderExpiryJob(VoucherOrderRepository orders, OrderService service, Clock clock,
            @Value("${higo.orders.timeout-minutes:30}") long timeoutMinutes) {
        this.orders=orders; this.service=service; this.clock=clock; this.timeoutMinutes=timeoutMinutes;
    }
    @Scheduled(fixedDelayString="${higo.orders.expiry-delay-ms:60000}")
    public void expire() {
        if(timeoutMinutes<=0) return;
        for(var order:orders.findTop100ByStatusAndCreatedAtBeforeOrderByIdAsc(
                OrderStatus.CREATED, LocalDateTime.now(clock).minusMinutes(timeoutMinutes))) {
            try { service.transition(order.getId(),order.getUserId(),false); }
            catch(com.higo.life.support.ConflictException ignored) { /* A payment won the lock. */ }
        }
    }
}
