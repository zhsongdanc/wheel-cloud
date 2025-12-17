package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.analytics.BucketInfo;
import com.wheel.cloud.hystrix.analytics.CircularList;
import com.wheel.cloud.hystrix.analytics.InvokeInfo;
import com.wheel.cloud.hystrix.analytics.Metrics;
import com.wheel.cloud.hystrix.enums.CircuitBreakerStatus;
import com.wheel.cloud.hystrix.spring.HystrixProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 负责
 */
@Slf4j
public class CircuitBreaker {

    private String methodKey;

    private CircularList circularList = new CircularList();

    private HystrixProperties properties;

    private AtomicReference<CircuitBreakerStatus> currentStatus = new AtomicReference<>(CircuitBreakerStatus.CLOSED);

    private Metrics metrics = new Metrics();

    // 半开状态下已经发送的请求数
    private AtomicInteger haveSendReqWhenHalfOpen = new AtomicInteger(0);

    private long lastStartOpenTimestamp = 0;


    /**
     * 以下情况需要记录：
     * 1. closed -> open
     * 2. half open -> open
     * 3. ?
     */
    private long coolDownTimestamp = 0;


    public CircuitBreaker(String methodKey, HystrixProperties hystrixProperties) {
        this.methodKey = methodKey;
        this.properties = hystrixProperties;
    }


    /**
     * 这里处理两个状态转换：（1）open -> half open 依赖于冷却期（2）half open -> open 依赖于最大允许请求次数
     * !! 注意：这里不能直接修改状态
     */
    public boolean allowRequest() {
        // 如果后续很久之后才进行探测，那么下游可能已经恢复了但是会失败一次
        if (currentStatus.get() == CircuitBreakerStatus.OPEN){
            // todo 为什么标准实现这里就可以探测
            if (coolDownTimePassed()){
                if (tryChangeOpenToHalfOpen()) {
                    haveSendReqWhenHalfOpen.incrementAndGet();
                    return true;
                };
            }
            return false;
        }
        if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN){
            boolean shouldPass = haveSendReqWhenHalfOpen.get() < properties.getHalfOpenTotalRequest();
            if (shouldPass){
                haveSendReqWhenHalfOpen.incrementAndGet();
                return true;
            } else {
                currentStatus.compareAndSet(CircuitBreakerStatus.HALF_OPEN, CircuitBreakerStatus.OPEN);
                haveSendReqWhenHalfOpen.set(0);
                lastStartOpenTimestamp = System.currentTimeMillis();
                metrics.reInitialization();
                return false;
            }
        }
        return true;
    }

    public boolean allRequestByBucket() {
        // 1. 删除过期数据
        circularList.clearExpiredBucket();
        // 2. 获取失败率判断是否允许通过
        float successRate = circularList.getSuccessRate();
        return true;
    }

    public CircuitBreakerStatus getStatus() {
        return currentStatus.get();
    }


    private boolean coolDownTimePassed() {
        return System.currentTimeMillis() > coolDownTimestamp + properties.getCoolDownTime();
    }

    /**  TODO 暂时不考虑一个请求耗时很久的case,否则会有很多边界条件
     * 状态流转分类：
     * 1. closed -> open
     * 3. half open -> closed
     * 4. half open -> open (暂不处理这个状态，由下一次请求时判断)
     */
    public void checkAndChangeStatus() {

        // 1. 清理超过时间窗口的数据
        metrics.clearAllBeforeTimestamp(System.currentTimeMillis() - properties.getTimeWindowWhenClosed());

        // 2.1 如果当前是关的需要检查是否改为关
        if (currentStatus.get() == CircuitBreakerStatus.CLOSED){
            boolean shouldOpen = metrics.getFailureRate() >= properties.getClosedToOpenFailedRatio()
                    && metrics.getFailedInvokeQueue().size() >= properties.getClosedToOpenMinTotalCount();
            if (shouldOpen && currentStatus.compareAndSet(CircuitBreakerStatus.CLOSED, CircuitBreakerStatus.OPEN)){
                coolDownTimestamp = System.currentTimeMillis();
                metrics.reInitialization();
            }
        } else if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN && haveSendReqWhenHalfOpen.get() > 0) {

            float successRatio = metrics.getSuccessCountWhenHalfOpen().get() / (float) haveSendReqWhenHalfOpen.get();
            if (successRatio >= properties.getHalfOpenToOpenMinSuccessRatio()
                    && metrics.getSuccessCountWhenHalfOpen().get() >= properties.getHalfOpenToOpenMinSuccessCount()){
                currentStatus.compareAndSet(CircuitBreakerStatus.HALF_OPEN, CircuitBreakerStatus.CLOSED);
                haveSendReqWhenHalfOpen.set(0);
            }
        }
    }



    public boolean tryChangeOpenToHalfOpen() {
        if (currentStatus.compareAndSet(CircuitBreakerStatus.OPEN, CircuitBreakerStatus.HALF_OPEN)){
            return true;
        }
        return false;
    }

    public boolean tryChangeHalfOpenToClosed() {
        if (currentStatus.compareAndSet(CircuitBreakerStatus.HALF_OPEN, CircuitBreakerStatus.CLOSED)){
            haveSendReqWhenHalfOpen.set(0);
            return true;
        }
        return false;
    }



    public void recordSuccess(InvokeInfo invokeInfo){
        if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN){
            metrics.getSuccessCountWhenHalfOpen().incrementAndGet();
        } else if (currentStatus.get() == CircuitBreakerStatus.CLOSED) {
            metrics.getSuccessInvokeQueue().add(invokeInfo);
        }
        checkAndChangeStatus();
    }

    public void recordFailed(InvokeInfo invokeInfo){
        if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN){
            metrics.getFailedCountWhenHalfOpen().incrementAndGet();
        } else if (currentStatus.get() == CircuitBreakerStatus.CLOSED) {
            metrics.getFailedInvokeQueue().add(invokeInfo);
        }
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
