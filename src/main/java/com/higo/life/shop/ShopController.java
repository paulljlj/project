package com.higo.life.shop;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shops")
public class ShopController {

    private final ShopService shopService;
    private final com.higo.life.auth.CurrentUser current;
    private final boolean baseline;

    public ShopController(ShopService shopService,com.higo.life.auth.CurrentUser current,@org.springframework.beans.factory.annotation.Value("${higo.baseline.enabled:false}") boolean baseline) {
        this.shopService = shopService;this.current=current;this.baseline=baseline;
    }

    @PostMapping
    public ResponseEntity<ShopResponse> create(@Valid @RequestBody CreateShopRequest request) {
        if(!baseline) current.require();
        Shop shop = shopService.create(
                request.name(), request.category(), request.address(), request.longitude(), request.latitude()
        );
        return ResponseEntity.created(URI.create("/api/shops/" + shop.getId()))
                .body(ShopResponse.from(shop));
    }

    @GetMapping("/{id}")
    public ShopResponse get(@PathVariable Long id) {
        return shopService.get(id);
    }

    @PutMapping("/{id}")
    public ShopResponse update(@PathVariable Long id, @Valid @RequestBody UpdateShopRequest request) {
        if(!baseline) current.require();
        return shopService.update(id, request);
    }

    @GetMapping
    public List<ShopResponse> list() {
        return shopService.list().stream().map(ShopResponse::from).toList();
    }

    @GetMapping("/categories") public List<String> categories() { return shopService.categories(); }
    @GetMapping("/by-name") public List<ShopResponse> byName(@RequestParam String name,@RequestParam(defaultValue="0") int page) { return shopService.byName(name,page); }
    @GetMapping("/{id}/hot") public ShopResponse logical(@PathVariable Long id) { return shopService.logical(id); }
    @GetMapping("/search")
    public List<ShopResponse> search(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double latitude
    ) {
        return shopService.search(category, page, longitude, latitude);
    }
}

