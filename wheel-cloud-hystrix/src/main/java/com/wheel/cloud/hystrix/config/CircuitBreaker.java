package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.enums.CircuitBreakerStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 负责
 */
@Slf4j
public class CircuitBreaker {

    private String methodKey;

    private AtomicReference<CircuitBreakerStatus> currentStatus = new AtomicReference<>(CircuitBreakerStatus.CLOSED);

    private Metrics metrics = new Metrics();


    public CircuitBreaker(String methodKey) {
        this.methodKey = methodKey;
    }


    public boolean allowRequest() {
        return metrics.getFailureRate() < CircuitBreakerConfig.failedThresholdOfCircuitOpen;
    }

    public CircuitBreakerStatus getStatus() {
        return currentStatus.get();
    }


    public void checkAndChangeStatus() {

        // 1. 清理超过时间窗口的数据
        metrics.clearAllBeforeTimestamp(System.currentTimeMillis());

        // 2. 根据当前数据判断是否要切换状态(还没实现半打开状态) 暂不实现
//        if (currentStatus.get() == CircuitBreakerStatus.CLOSED){
//            boolean shouldOpen = metrics.getFailureRate() >= CircuitBreakerConfig.failedThresholdOfCircuitOpen;
//            if (shouldOpen){
//                currentStatus = CircuitBreakerStatus.OPEN;
//            }
//        }
    }





    public void recordSuccess(InvokeInfo invokeInfo){
        metrics.getSuccessInvokeQueue().add(invokeInfo);
        checkAndChangeStatus();
    }

    public void recordFailed(InvokeInfo invokeInfo){
        metrics.getFailedInvokeQueue().add(invokeInfo);
        checkAndChangeStatus();

    }

    public void forceOpen(){
        currentStatus.set(CircuitBreakerStatus.OPEN);
        // 清除数据
        metrics.reInitialization();
    }

    public void forceClose(){
        currentStatus.set(CircuitBreakerStatus.CLOSED);
    }

    public void forceHalfOpen(){
        currentStatus.set(CircuitBreakerStatus.HALF_OPEN);
    }

}
