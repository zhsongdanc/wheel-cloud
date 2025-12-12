package com.wheel.cloud.hystrix.config;

public class CircuitBreakerConfig {
    // 熔断器关闭状态下的时间窗口
    public static final long timeWindowWhenClosed = 10*1000;
    // 熔断器关闭状态下的失败比例阈值，达到这个比例断路器打开
    public static final float closedToOpenFailedRatio = 0.5f;
    // 熔断器关闭状态下的失败数量阈值，达到这个数量断路器打开
    public static final int closedToOpenMinTotalCount = 10;


    // 探测冷却期，每隔该时间才会转为half open 去探测
    public static final long coolDownTime = 5*1000;

    // half open状态最久持续时间，超出后强制改为OPEN状态
    public static final int halfOpenToOpenMaxTimeWindow = 3*1000;
    // half open状态可以发送的总的请求总数
    public static final int halfOpenTotalRequest = 20;
    // half open状态成功请求数阈值
    public static final int halfOpenToOpenMinSuccessCount = 3;
    // half open状态成功请求比例阈值
    public static final float halfOpenToOpenMinSuccessRatio = 0.75f;
}
