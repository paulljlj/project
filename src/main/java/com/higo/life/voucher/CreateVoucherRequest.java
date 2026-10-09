package com.higo.life.voucher;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateVoucherRequest(
        @NotNull @Positive Long shopId,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 500) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @PositiveOrZero int stock,
        @NotNull LocalDateTime beginAt,
        @NotNull LocalDateTime endAt
) {
}

