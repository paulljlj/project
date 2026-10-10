package com.higo.life.voucher;

import com.higo.life.seckill.SeckillService;
import com.higo.life.shop.ShopRepository;
import com.higo.life.support.InvalidRequestException;
import com.higo.life.support.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final ShopRepository shopRepository;
    private final ObjectProvider<SeckillService> seckillService;

    public VoucherService(
            VoucherRepository voucherRepository,
            ShopRepository shopRepository,
            ObjectProvider<SeckillService> seckillService
    ) {
        this.voucherRepository = voucherRepository;
        this.shopRepository = shopRepository;
        this.seckillService = seckillService;
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
        Voucher voucher = voucherRepository.save(
                new Voucher(shopId, title, description, price, stock, beginAt, endAt)
        );
        SeckillService service = seckillService.getIfAvailable();
        if (service != null) {
            com.higo.life.support.AfterCommit.run(()->service.prepare(voucher));
        }
        return voucher;
    }

    @Transactional(readOnly = true)
    public List<Voucher> listByShop(Long shopId) {
        if (!shopRepository.existsById(shopId)) {
            throw new NotFoundException("商家不存在: " + shopId);
        }
        return voucherRepository.findByShopIdOrderByIdAsc(shopId);
    }
}
