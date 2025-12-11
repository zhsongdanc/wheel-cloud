package com.wheel.cloud.hystrix.config;

public class CircuitBreakerConfig {
    public static final long timeWindowSize = 10*1000;
    // 失败阈值
    public static final float failedThresholdOfCircuitOpen = 0.5f;

    // 少于该次数，不允许熔断
    public static final int minFailedThresholdCount = 10;

    // half open状态下每次允许的最大探测次数
    public static final int maxProbeCount = 5;

    // 首次open或探测一次后多久不允许再次探测
    public static final long coolDownTimeWhenHalfOpen = 2*1000;
}
