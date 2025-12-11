package com.wheel.cloud.hystrix.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CircuitBreakerManager {
    private Map<String /*methodKey*/, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();


    public CircuitBreaker getCircuitBreaker(String methodKey) {
        return circuitBreakerMap.computeIfAbsent(methodKey, k -> new CircuitBreaker(methodKey));
    }
}
