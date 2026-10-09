package com.higo.life.shop;

import java.time.LocalDateTime;

public record ShopResponse(
        Long id,
        String name,
        String category,
        String address,
        Double longitude,
        Double latitude,
        Double distanceMeters,
        LocalDateTime createdAt
) {
    static ShopResponse from(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getCategory(),
                shop.getAddress(),
                shop.getLongitude(),
                shop.getLatitude(),
                null,
                shop.getCreatedAt()
        );
    }

    ShopResponse withDistance(Double distanceMeters) {
        return new ShopResponse(id, name, category, address, longitude, latitude, distanceMeters, createdAt);
    }
}

