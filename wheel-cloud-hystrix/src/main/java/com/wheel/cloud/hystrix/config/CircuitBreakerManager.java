package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.spring.HystrixProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class CircuitBreakerManager {

    @Resource
    private HystrixProperties hystrixProperties;

    private Map<String /*methodKey*/, CircuitBreaker> circuitBreakerMap = new ConcurrentHashMap<>();


    public CircuitBreaker getCircuitBreaker(String methodKey) {
        return circuitBreakerMap.computeIfAbsent(methodKey, k -> new CircuitBreaker(methodKey, hystrixProperties));
    }
}
