package com.higo.life.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.common.TopicPartition;
@Configuration @ConditionalOnProperty(name="higo.kafka.enabled",havingValue="true",matchIfMissing=true)
public class KafkaFailureConfig {
    @Bean public DefaultErrorHandler orderErrorHandler(KafkaTemplate<String,Object> template) {
        var recoverer=new DeadLetterPublishingRecoverer(template,(record,error)->new TopicPartition(record.topic()+".DLT",record.partition()));
        recoverer.setFailIfSendResultIsError(true);
        var handler=new DefaultErrorHandler(recoverer,new FixedBackOff(1000L,3L));
        return handler;
    }
    @Bean public org.apache.kafka.clients.admin.NewTopic deadLetters(@org.springframework.beans.factory.annotation.Value("${higo.kafka.order-topic:higo.seckill.orders.v1}") String topic) {
        return org.springframework.kafka.config.TopicBuilder.name(topic+".DLT").partitions(3).replicas(1).build();
    }
}
