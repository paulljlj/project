package com.higo.life.seckill;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.concurrent.TimeUnit;
@Component @ConditionalOnProperty(name="higo.kafka.enabled",havingValue="true",matchIfMissing=true)
public class OrderOutboxDispatcher {
    private final OrderRequestRepository requests; private final KafkaTemplate<String,Object> kafka;
    private final TransactionTemplate tx; private final String topic;
    public OrderOutboxDispatcher(OrderRequestRepository requests,KafkaTemplate<String,Object> kafka,PlatformTransactionManager manager,@Value("${higo.kafka.order-topic:higo.seckill.orders.v1}") String topic) {
        this.requests=requests;this.kafka=kafka;this.tx=new TransactionTemplate(manager);this.topic=topic;
    }
    @Scheduled(fixedDelayString="${higo.outbox.delay-ms:1000}") public void dispatch() {
        for(var candidate:requests.findTop20BySentFalseAndStatusOrderByCreatedAtAsc("ACCEPTED")) {
            tx.executeWithoutResult(status->{
                var r=requests.lockById(candidate.getId()).orElse(null);
                if(r==null || r.isSent() || !r.getStatus().equals("ACCEPTED")) return;
                try {
                    kafka.send(topic,r.getVoucherId().toString(),new SeckillOrderMessage(r.getId(),r.getUserId(),r.getVoucherId())).get(5,TimeUnit.SECONDS);
                    r.dispatched();
                } catch(InterruptedException ex) { Thread.currentThread().interrupt();r.sendFailed("发送被中断"); }
                catch(Exception ex) { r.sendFailed(ex.getClass().getSimpleName()); }
                // Timeout is ambiguous: keep durable request for retry; never release its reservation.
            });
        }
    }
}
