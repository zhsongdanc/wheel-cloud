package com.wheel.cloud.hystrix.config;

public class CircuitBreakerConfig {
    public static final long timeWindowSize = 10*1000;
    // 失败阈值
    public static final float failedThresholdOfCircuitOpen = 0.5f;

    // 少于该次数，不允许熔断
    public static final int minFailedThresholdCount = 10;

    // 探测冷却期
    public static final long coolDownTime = 2*1000;

    public static final int requestThresholdForHalfOpen = 5;
}
