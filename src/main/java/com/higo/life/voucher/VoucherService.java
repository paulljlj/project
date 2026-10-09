package com.higo.life.voucher;

import com.higo.life.shop.ShopRepository;
import com.higo.life.support.InvalidRequestException;
import com.higo.life.support.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final ShopRepository shopRepository;

    public VoucherService(VoucherRepository voucherRepository, ShopRepository shopRepository) {
        this.voucherRepository = voucherRepository;
        this.shopRepository = shopRepository;
    }

    @Transactional
    public Voucher create(
            Long shopId,
            String title,
            String description,
            BigDecimal price,
            int stock,
            LocalDateTime beginAt,
            LocalDateTime endAt
    ) {
        if (!shopRepository.existsById(shopId)) {
            throw new NotFoundException("商家不存在: " + shopId);
        }
        if (!endAt.isAfter(beginAt)) {
            throw new InvalidRequestException("优惠券结束时间必须晚于开始时间");
        }
        return voucherRepository.save(
                new Voucher(shopId, title, description, price, stock, beginAt, endAt)
        );
    }

    @Transactional(readOnly = true)
    public List<Voucher> listByShop(Long shopId) {
        if (!shopRepository.existsById(shopId)) {
            throw new NotFoundException("商家不存在: " + shopId);
        }
        return voucherRepository.findByShopIdOrderByIdAsc(shopId);
    }
}

