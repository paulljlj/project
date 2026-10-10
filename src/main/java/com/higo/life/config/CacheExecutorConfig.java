package com.higo.life.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration
public class CacheExecutorConfig {
    @Bean("cacheRebuildExecutor") public ThreadPoolTaskExecutor cacheRebuildExecutor() {
        var e=new ThreadPoolTaskExecutor(); e.setCorePoolSize(2); e.setMaxPoolSize(4); e.setQueueCapacity(64);
        e.setThreadNamePrefix("cache-rebuild-"); e.setWaitForTasksToCompleteOnShutdown(true); e.initialize(); return e;
    }
}
