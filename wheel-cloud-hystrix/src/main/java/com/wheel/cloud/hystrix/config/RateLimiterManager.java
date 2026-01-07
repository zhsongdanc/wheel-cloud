package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.limit.FixedWindowLimiter;
import com.wheel.cloud.hystrix.limit.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimiterManager {


    private final Map<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();


    public  RateLimiter getRateLimiter(String key, int algorithm, int permitsPerSecond) {
        return rateLimiterMap.computeIfAbsent(key, k -> createRateLimiter(algorithm, permitsPerSecond));
    }

    public RateLimiter createRateLimiter(int algorithm, int permitsPerSecond) {
        if (algorithm == 1) {
            return new FixedWindowLimiter(permitsPerSecond);
        }

        return new FixedWindowLimiter(permitsPerSecond);
    }
}
