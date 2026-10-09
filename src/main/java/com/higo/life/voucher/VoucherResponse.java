package com.higo.life.voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VoucherResponse(
        Long id,
        Long shopId,
        String title,
        String description,
        BigDecimal price,
        int stock,
        LocalDateTime beginAt,
        LocalDateTime endAt
) {
    public static VoucherResponse from(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getShopId(),
                voucher.getTitle(),
                voucher.getDescription(),
                voucher.getPrice(),
                voucher.getStock(),
                voucher.getBeginAt(),
                voucher.getEndAt()
        );
    }
}

