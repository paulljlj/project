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
    private final com.higo.life.seckill.OrderRequestRepository requests;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;

    public OrderService(
            VoucherOrderRepository orderRepository,
            VoucherRepository voucherRepository,
            Clock clock,com.higo.life.seckill.OrderRequestRepository requests,org.springframework.data.redis.core.StringRedisTemplate redis
    ) {
        this.orderRepository = orderRepository;
        this.voucherRepository = voucherRepository;
        this.clock = clock;this.requests=requests;this.redis=redis;
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

    @Transactional
    public VoucherOrder placeReserved(String requestId, Long userId, Long voucherId) {
        Voucher voucher = voucherRepository.lockById(voucherId)
                .orElseThrow(() -> new NotFoundException("优惠券不存在"));
        var request=requests.lockById(requestId).orElse(null);
        if(request!=null && (!request.getUserId().equals(userId)||!request.getVoucherId().equals(voucherId))) throw new ConflictException("消息与请求不匹配");
        VoucherOrder processed = orderRepository.findByRequestId(requestId).orElse(null);
        if (processed != null) {
            if(request!=null) { request.completed(); requests.save(request); }
            return processed;
        }
        VoucherOrder existing = orderRepository.findByUserIdAndVoucherId(userId, voucherId).orElse(null);
        if (existing != null) {
            if(request!=null) { request.completed(); requests.save(request); }
            return existing;
        }
        int changedRows = voucherRepository.decrementStock(voucherId);
        if (changedRows == 0) {
            throw new ConflictException("数据库库存不足");
        }
        if(request!=null) { request.completed(); requests.save(request); }
        return orderRepository.saveAndFlush(new VoucherOrder(requestId, userId, voucherId, voucher.getPrice()));
    }

    public java.util.List<VoucherOrder> mine(Long userId) { return orderRepository.findByUserIdOrderByIdDesc(userId); }
    @Transactional public VoucherOrder transition(Long id,Long userId,boolean payment) {
        var o=orderRepository.lockById(id).orElseThrow(()->new NotFoundException("订单不存在"));
        if(!o.getUserId().equals(userId)) throw new NotFoundException("订单不存在");
        if(payment && o.getStatus()==OrderStatus.PAID || !payment && o.getStatus()==OrderStatus.CANCELLED) return o;
        if(o.getStatus()!=OrderStatus.CREATED) throw new ConflictException("订单状态不允许此操作");
        if(payment) o.pay(); else {
            o.cancel(); voucherRepository.restoreStock(o.getVoucherId());
            com.higo.life.support.AfterCommit.run(()->redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<Long>(
                "if redis.call('EXISTS',KEYS[1])==1 then return redis.call('INCR',KEYS[1]) else return 0 end",Long.class),
                java.util.List.of(com.higo.life.cache.CacheKeys.SECKILL_STOCK+o.getVoucherId())));
        }
        return orderRepository.save(o);
    }
    @Transactional(readOnly = true)
    public VoucherOrder get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("订单不存在: " + id));
    }

    @Transactional(readOnly = true)
    public VoucherOrder getByRequestId(String requestId) {
        return orderRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException("订单请求尚未完成: " + requestId));
    }
}

