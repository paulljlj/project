package com.higo.life.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShopRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(max = 255) String address
) {
}

