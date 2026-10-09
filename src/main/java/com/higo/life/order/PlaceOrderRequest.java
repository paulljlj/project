package com.higo.life.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaceOrderRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long voucherId
) {
}

