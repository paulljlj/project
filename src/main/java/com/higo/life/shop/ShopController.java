package com.higo.life.shop;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @PostMapping
    public ResponseEntity<ShopResponse> create(@Valid @RequestBody CreateShopRequest request) {
        Shop shop = shopService.create(request.name(), request.category(), request.address());
        return ResponseEntity.created(URI.create("/api/shops/" + shop.getId()))
                .body(ShopResponse.from(shop));
    }

    @GetMapping("/{id}")
    public ShopResponse get(@PathVariable Long id) {
        return ShopResponse.from(shopService.get(id));
    }

    @GetMapping
    public List<ShopResponse> list() {
        return shopService.list().stream().map(ShopResponse::from).toList();
    }
}

