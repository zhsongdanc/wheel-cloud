package com.wheel.cloud.hystrix.config;

import com.wheel.cloud.hystrix.analytics.BucketManager;
import com.wheel.cloud.hystrix.analytics.InvokeInfo;
import com.wheel.cloud.hystrix.enums.CircuitBreakerStatus;
import com.wheel.cloud.hystrix.spring.HystrixProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * （1）如果当前状态是半开，只有所有探测请求全部探测成功才修改状态为关闭，否则直接将断路器状态改为打开
 */
@Slf4j
public class CircuitBreaker {

    private final String methodKey;

    private BucketManager bucketManager = new BucketManager();

    private HystrixProperties properties;

    private AtomicReference<CircuitBreakerStatus> currentStatus = new AtomicReference<>(CircuitBreakerStatus.CLOSED);

    // 半开状态下已经发送的请求数
    private AtomicInteger haveSendReqWhenHalfOpen = new AtomicInteger(0);
    private AtomicInteger successfulReqWhenHalfOpen = new AtomicInteger(0);
    private AtomicInteger failedReqWhenHalfOpen = new AtomicInteger(0);
    private AtomicInteger completedReqWhenHalfOpen = new AtomicInteger(0);

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
            // todo 为什么标准实现这里就可以探测,因为要不然没地方判断
            if (coolDownTimePassed()){
                if (tryChangeOpenToHalfOpen()) {
                    haveSendReqWhenHalfOpen.incrementAndGet();
                    return true;
                };
            }
            return false;
        }
        if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN){
            boolean allow =  haveSendReqWhenHalfOpen.get() < properties.getHalfOpenTotalRequest();
            if (allow){
                haveSendReqWhenHalfOpen.incrementAndGet();
            }
            return allow;
        }
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

    public void changeStatusWhenSuccess(long startTime) {
        // 半开 -> 关闭(当前只有所有探测请求全部返回后才修改状态)
        if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN && haveSendReqWhenHalfOpen.get() > 0
                && completedReqWhenHalfOpen.get() >= haveSendReqWhenHalfOpen.get()) {

            float successRatio = successfulReqWhenHalfOpen.get() / (float) haveSendReqWhenHalfOpen.get();
            if (successRatio >= properties.getHalfOpenToOpenMinSuccessRatio()
                    && successfulReqWhenHalfOpen.get() >= properties.getHalfOpenToOpenMinSuccessCount()){
                currentStatus.compareAndSet(CircuitBreakerStatus.HALF_OPEN, CircuitBreakerStatus.CLOSED);
                clearHalfOpenMetrics();
            }
        }
    }

    public void changeStatusWhenFailed(long startTime) {
        // 2. 关闭 -> 开
        if (currentStatus.get() == CircuitBreakerStatus.CLOSED){
            float successRate = bucketManager.computeAndGetSuccessRate(startTime);

            boolean shouldOpen = 1 - successRate >= properties.getClosedToOpenFailedRatio();
            if (shouldOpen && currentStatus.compareAndSet(CircuitBreakerStatus.CLOSED, CircuitBreakerStatus.OPEN)){
                coolDownTimestamp = System.currentTimeMillis();
                bucketManager.clearAll();
                clearHalfOpenMetrics();
            }
        } else if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN) { //  半开 -> 打开
            boolean changeHalfOpen2Open = currentStatus.compareAndSet(CircuitBreakerStatus.HALF_OPEN, CircuitBreakerStatus.OPEN);
            if (changeHalfOpen2Open){
                coolDownTimestamp = System.currentTimeMillis();
            }
        }

    }


    private void clearHalfOpenMetrics() {
        haveSendReqWhenHalfOpen.set(0);
        successfulReqWhenHalfOpen.set(0);
        failedReqWhenHalfOpen.set(0);
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



    // （1）修改半开状态数据（2）修改统计数据（3）是否重置冷却期 （4）状态转换
    public void recordSuccess(InvokeInfo invokeInfo){
        completedReqWhenHalfOpen.incrementAndGet();
        if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN){
            successfulReqWhenHalfOpen.incrementAndGet();
        } else if (currentStatus.get() == CircuitBreakerStatus.CLOSED) {
            bucketManager.recordSingle(invokeInfo.getStartTime(), true);
        }
        // 半开->关闭；
        changeStatusWhenSuccess(invokeInfo.getStartTime());
    }

    // （1）修改半开状态数据（2）修改统计数据（3）是否重置冷却期 （4）状态转换
    public void recordFailed(InvokeInfo invokeInfo){
        completedReqWhenHalfOpen.incrementAndGet();
        if (currentStatus.get() == CircuitBreakerStatus.HALF_OPEN){
            failedReqWhenHalfOpen.incrementAndGet();
        } else if (currentStatus.get() == CircuitBreakerStatus.CLOSED) {
            bucketManager.recordSingle(invokeInfo.getStartTime(), false);
        }
        changeStatusWhenFailed(invokeInfo.getStartTime());
    }

    public void forceClose(){
        currentStatus.set(CircuitBreakerStatus.CLOSED);
    }

    public void forceHalfOpen(){
        currentStatus.set(CircuitBreakerStatus.HALF_OPEN);
    }

}
