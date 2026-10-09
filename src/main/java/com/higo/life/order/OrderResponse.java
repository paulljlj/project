package com.higo.life.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long userId,
        Long voucherId,
        BigDecimal amount,
        OrderStatus status,
        LocalDateTime createdAt
) {
    static OrderResponse from(VoucherOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getVoucherId(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}

