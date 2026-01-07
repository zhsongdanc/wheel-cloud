package com.wheel.cloud.hystrix.limit;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class FixedWindowLimiter implements RateLimiter {

    // 默认是1s作为固定窗口
    private static final long DEFAULT_WINDOW_SIZE = 1;

    private long permits;

    // 以毫秒为单位
    private long windowSize;

    private AtomicLong windowStartTime;

    private LongAdder counter;

    public FixedWindowLimiter(int permitsPerSecond) {
        this(permitsPerSecond, DEFAULT_WINDOW_SIZE);
    }

    public FixedWindowLimiter(int permits, long second) {
        this.permits = permits;
        this.windowStartTime = new AtomicLong(System.currentTimeMillis());
        this.counter = new LongAdder();
        this.windowSize = second*1000;
    }



    public boolean allowRequest(){
        // 如果超出window，重置windowStartTime和counter，否则累积counter
        long now = System.currentTimeMillis();
        long currentWindowStartTime = windowStartTime.get();
        if (now - currentWindowStartTime > windowSize) {
            if (windowStartTime.compareAndSet(currentWindowStartTime, now)) {
                counter.reset();
                counter.increment();
                return true;
            } else {
                if (counter.sum() < permits) {
                    counter.increment();
                    return true;
                }
                return false;
            }

        }
        if (counter.sum() < permits) {
            counter.increment();
            return true;
        }
        return false;
    }
}
