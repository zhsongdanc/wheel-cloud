package com.wheel.cloud.hystrix.limit;

public interface RateLimiter {

    boolean allowRequest();
}
