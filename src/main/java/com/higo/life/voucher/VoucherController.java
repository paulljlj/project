package com.higo.life.voucher;

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
@RequestMapping("/api")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping("/vouchers")
    public ResponseEntity<VoucherResponse> create(@Valid @RequestBody CreateVoucherRequest request) {
        Voucher voucher = voucherService.create(
                request.shopId(),
                request.title(),
                request.description(),
                request.price(),
                request.stock(),
                request.beginAt(),
                request.endAt()
        );
        return ResponseEntity.created(URI.create("/api/vouchers/" + voucher.getId()))
                .body(VoucherResponse.from(voucher));
    }

    @GetMapping("/shops/{shopId}/vouchers")
    public List<VoucherResponse> listByShop(@PathVariable Long shopId) {
        return voucherService.listByShop(shopId).stream().map(VoucherResponse::from).toList();
    }
}

