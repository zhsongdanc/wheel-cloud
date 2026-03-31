package com.wheel.cloud.eureka.registry.task;

import com.wheel.cloud.eureka.registry.model.RegistryInstanceView;
import com.wheel.cloud.eureka.registry.store.InMemoryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EvictionTask {

    private static final Logger log = LoggerFactory.getLogger(EvictionTask.class);

    private final InMemoryRegistry registry;

    public EvictionTask(InMemoryRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelayString = "${registry.eviction-interval-ms:5000}")
    public void evictExpiredInstances() {
        // 注册中心拿不到可靠的死亡通知，所以只能周期性清理长期未续约实例。
        List<RegistryInstanceView> evicted = registry.evictExpiredInstances();
        if (!evicted.isEmpty()) {
            log.info("evicted {} expired instance(s)", evicted.size());
        }
    }
}
