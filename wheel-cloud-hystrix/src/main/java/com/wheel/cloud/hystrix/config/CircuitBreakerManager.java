package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.property.CommandProperty;
import com.wheel.cloud.hystrix.property.ThreadPoolProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CircuitBreakerManager {

    @Resource
    private CommandProperty commandProperty;

    private Map<String /*methodKey*/, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();

    private Map<String /*groupKey*/, ExecutorService> executorMap = new ConcurrentHashMap<>();

    private Map<String /*groupKey*/, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

    public CircuitBreaker getCircuitBreaker(String methodKey) {
        return circuitBreakerMap.computeIfAbsent(methodKey, k -> new CircuitBreaker(methodKey, commandProperty));
    }

    public ExecutorService getExecutor(String groupKey, ThreadPoolProperty threadPoolProperty) {

        return executorMap.computeIfAbsent(groupKey,
                k -> new ThreadPoolExecutor(threadPoolProperty.getCoreSize(), threadPoolProperty.getMaxSize(),
                        threadPoolProperty.getKeepAliveTimeSeconds(), TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(threadPoolProperty.getQueueSize())));
    }

    public Semaphore getSemaphore(String groupKey, int maxConcurrentRequests) {
        return semaphoreMap.computeIfAbsent(groupKey, k -> new Semaphore(maxConcurrentRequests));
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
