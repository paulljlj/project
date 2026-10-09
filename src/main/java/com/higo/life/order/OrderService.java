package com.higo.life.order;

import com.higo.life.support.ConflictException;
import com.higo.life.support.NotFoundException;
import com.higo.life.voucher.Voucher;
import com.higo.life.voucher.VoucherRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final VoucherOrderRepository orderRepository;
    private final VoucherRepository voucherRepository;
    private final Clock clock;

    public OrderService(
            VoucherOrderRepository orderRepository,
            VoucherRepository voucherRepository,
            Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.voucherRepository = voucherRepository;
        this.clock = clock;
    }

    @Transactional
    public VoucherOrder place(Long userId, Long voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new NotFoundException("优惠券不存在: " + voucherId));

        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(voucher.getBeginAt())) {
            throw new ConflictException("秒杀尚未开始");
        }
        if (now.isAfter(voucher.getEndAt())) {
            throw new ConflictException("秒杀已经结束");
        }
        if (orderRepository.existsByUserIdAndVoucherId(userId, voucherId)) {
            throw new ConflictException("同一用户不能重复购买同一张优惠券");
        }

        int changedRows = voucherRepository.decrementStock(voucherId);
        if (changedRows == 0) {
            throw new ConflictException("优惠券库存不足");
        }

        return orderRepository.save(new VoucherOrder(userId, voucherId, voucher.getPrice()));
    }

    @Transactional(readOnly = true)
    public VoucherOrder get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("订单不存在: " + id));
    }
}

