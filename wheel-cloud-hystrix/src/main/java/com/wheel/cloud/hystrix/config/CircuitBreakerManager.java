package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.spring.HystrixProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CircuitBreakerManager {

    private static final int DEFAULT_CORE_SIZE = 10;
    private static final int DEFAULT_MAX_SIZE = 20;
    private static final int DEFAULT_KEEP_ALIVE_TIME = 100;
    private static final int DEFAULT_QUEUE_SIZE = 100;

    @Resource
    private HystrixProperties hystrixProperties;

    private Map<String /*methodKey*/, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();

    private Map<String /*methodKey*/, ExecutorService> executorMap = new ConcurrentHashMap<>();

    public CircuitBreaker getCircuitBreaker(String methodKey) {
        return circuitBreakerMap.computeIfAbsent(methodKey, k -> new CircuitBreaker(methodKey, hystrixProperties));
    }

    public ExecutorService getExecutor(String methodKey) {
        return executorMap.computeIfAbsent(methodKey,
                k -> new ThreadPoolExecutor(DEFAULT_CORE_SIZE, DEFAULT_MAX_SIZE, DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new ArrayBlockingQueue<>(DEFAULT_QUEUE_SIZE)));
    }

    @PreDestroy
    public void destroy() {
        executorMap.values().forEach(executorService -> {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    executorService.awaitTermination(5, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

        });
    }
}
