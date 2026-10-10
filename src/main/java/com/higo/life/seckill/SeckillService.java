package com.higo.life.seckill;
import com.higo.life.auth.CurrentUser;
import com.higo.life.cache.CacheKeys;
import com.higo.life.support.*;
import com.higo.life.voucher.*;
import com.higo.life.order.VoucherOrderRepository;
import java.time.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
@Service @ConditionalOnProperty(name={"higo.redis.enabled","higo.kafka.enabled"},havingValue="true",matchIfMissing=true)
public class SeckillService {
    private static final DefaultRedisScript<Long> RESERVE=new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> UNDO=new DefaultRedisScript<>();
    static { RESERVE.setLocation(new ClassPathResource("seckill_reserve.lua"));RESERVE.setResultType(Long.class);UNDO.setLocation(new ClassPathResource("seckill_compensate.lua"));UNDO.setResultType(Long.class); }
    private final StringRedisTemplate redis;private final VoucherRepository vouchers;private final VoucherOrderRepository orders;
    private final OrderRequestRepository requests;private final CurrentUser current;private final Clock clock;
    public SeckillService(StringRedisTemplate redis,VoucherRepository vouchers,VoucherOrderRepository orders,OrderRequestRepository requests,CurrentUser current,Clock clock) {
        this.redis=redis;this.vouchers=vouchers;this.orders=orders;this.requests=requests;this.current=current;this.clock=clock;
    }
    public void prepare(Voucher v) {
        Duration ttl=Duration.between(LocalDateTime.now(clock),v.getEndAt()).plusHours(1);
        if(!ttl.isNegative() && !ttl.isZero()) redis.opsForValue().setIfAbsent(CacheKeys.SECKILL_STOCK+v.getId(),Integer.toString(v.getStock()),ttl);
    }
    @Transactional public SeckillReservationResponse reserve(Long voucherId) {
        Long user=current.require().id();
        // The row lock also serializes Redis recovery with reservations and database consumers.
        Voucher v=vouchers.lockById(voucherId).orElseThrow(()->new NotFoundException("优惠券不存在"));
        LocalDateTime now=LocalDateTime.now(clock);
        if(now.isBefore(v.getBeginAt())) throw new ConflictException("秒杀尚未开始");
        if(!now.isBefore(v.getEndAt())) throw new ConflictException("秒杀已经结束");
        if(requests.existsByUserIdAndVoucherId(user,voucherId)||orders.existsByUserIdAndVoucherId(user,voucherId)) throw new ConflictException("同一用户不能重复购买同一张优惠券");
        String stock=CacheKeys.SECKILL_STOCK+voucherId,buyers=CacheKeys.SECKILL_BUYERS+voucherId;
        if(!Boolean.TRUE.equals(redis.hasKey(stock))) reconcile(v);
        Long result=redis.execute(RESERVE,List.of(stock,buyers),user.toString());
        if(Long.valueOf(1).equals(result)) throw new ConflictException("优惠券库存不足");
        if(Long.valueOf(2).equals(result)) throw new ConflictException("同一用户不能重复购买同一张优惠券");
        if(!Long.valueOf(0).equals(result)) throw new ConflictException("库存尚未就绪");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if(status==STATUS_ROLLED_BACK) redis.execute(UNDO,List.of(stock,buyers),user.toString());
            }
        });
        String id=UUID.randomUUID().toString();requests.saveAndFlush(new OrderRequest(id,user,voucherId));
        return new SeckillReservationResponse(id,"ACCEPTED");
    }
    private void reconcile(Voucher v) {
        var pending=requests.findByVoucherId(v.getId()).stream().filter(r->!r.getStatus().equals("COMPLETED")).toList();
        int available=Math.max(0,v.getStock()-pending.size());
        var users=new HashSet<String>();orders.findByVoucherId(v.getId()).forEach(o->users.add(o.getUserId().toString()));
        requests.findByVoucherId(v.getId()).forEach(r->users.add(r.getUserId().toString()));
        String buyers=CacheKeys.SECKILL_BUYERS+v.getId();
        redis.delete(buyers);if(!users.isEmpty()) redis.opsForSet().add(buyers,users.toArray(String[]::new));
        Duration ttl=Duration.between(LocalDateTime.now(clock),v.getEndAt()).plusHours(1);
        if(ttl.isNegative() || ttl.isZero()) ttl=Duration.ofHours(1);
        redis.opsForValue().set(CacheKeys.SECKILL_STOCK+v.getId(),Integer.toString(available),ttl);redis.expire(buyers,ttl);
    }
    @Transactional public void reconcile(Long voucherId) {
        current.require();reconcile(vouchers.lockById(voucherId).orElseThrow(()->new NotFoundException("优惠券不存在")));
    }
    public record RequestState(String requestId,String status,int attempts,Long orderId) {}
    public RequestState state(String id) {
        var r=requests.findById(id).orElseThrow(()->new NotFoundException("订单请求不存在"));
        if(!r.getUserId().equals(current.require().id())) throw new NotFoundException("订单请求不存在");
        return new RequestState(id,r.getStatus(),r.getAttempts(),orders.findByRequestId(id).map(o->o.getId()).orElse(null));
    }
    @Transactional public void replay(String id) {
        var r=requests.lockById(id).orElseThrow(()->new NotFoundException("订单请求不存在"));
        if(!r.getUserId().equals(current.require().id())) throw new NotFoundException("订单请求不存在");
        if(!r.getStatus().equals("FAILED")) throw new ConflictException("只有失败请求可以重试"); r.replay();
    }
}
