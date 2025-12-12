package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.analytics.InvokeInfo;
import com.wheel.cloud.hystrix.analytics.Metrics;
import com.wheel.cloud.hystrix.enums.CircuitBreakerStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 负责
 */
@Slf4j
public class CircuitBreaker {

    private String methodKey;

    private AtomicReference<CircuitBreakerStatus> currentStatus = new AtomicReference<>(CircuitBreakerStatus.CLOSED);

    private Metrics metrics = new Metrics();

    // 半开状态下如果请求通过了意味着什么，要采取什么动作
    // 什么时候重置这个计数器
    private AtomicInteger halfOpenCounter = new AtomicInteger(0);


    private long coolDownTimestamp = 0;


    public CircuitBreaker(String methodKey) {
        this.methodKey = methodKey;
    }


    public boolean allowRequest() {
        // 如果后续很久之后才进行探测，那么下游可以已经恢复了但是会失败一次
        if (currentStatus.get() == CircuitBreakerStatus.OPEN){
            if (coolDownTimePassed()){
                if (tryChangeOpenToHalfOpen()) {
                    halfOpenCounter.set(0);
                };
            }
            return false;
        }
        if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN){
            return halfOpenCounter.getAndIncrement() < CircuitBreakerConfig.requestThresholdForHalfOpen;
        }
        return true;
    }

    public CircuitBreakerStatus getStatus() {
        return currentStatus.get();
    }


    private boolean coolDownTimePassed() {
        return System.currentTimeMillis() > coolDownTimestamp + CircuitBreakerConfig.coolDownTime;
    }

    /**
     * 状态流转分类：
     * 1. closed -> open
     * 2. open -> half open
     * 3. half open -> closed
     * 4. half open -> open
     */
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
        // 如果修改为open后需要重置coolDownTimestamp；如果冷却期过了需要重置coolDownTimestamp
    }



    public boolean tryChangeOpenToHalfOpen() {
        if (currentStatus.compareAndSet(CircuitBreakerStatus.OPEN, CircuitBreakerStatus.HALF_OPEN)){
            halfOpenCounter.set(0);
            return true;
        }
        return false;
    }

    public boolean tryChangeHalfOpenToClosed() {
        if (currentStatus.compareAndSet(CircuitBreakerStatus.HALF_OPEN, CircuitBreakerStatus.CLOSED)){
            halfOpenCounter.set(0);
            return true;
        }
        return false;
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
