package com.wheel.cloud.hystrix.limit;

public class LeakBucketLimiter implements RateLimiter {

    private static long DEFAULT_CAPACITY = 1000;

    private long capacity;

    // 漏出qps
    private long leakingRate;

    private long lastLeakingTime;

    private long water;

    public LeakBucketLimiter(long leakingRate) {
        this(DEFAULT_CAPACITY, leakingRate);
    }

    public LeakBucketLimiter(long capacity, long leakingRate) {
        this.capacity = capacity;
        this.leakingRate = leakingRate;
        this.lastLeakingTime = System.currentTimeMillis();
        this.water = 0;
    }

    @Override
    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();

        long passedTime = now - lastLeakingTime;
        long leakingWater = passedTime * leakingRate / 1000;

        // 只要漏出了水，就可以接收新请求
        if (leakingWater > 0) {
            water = Math.max(0, water - leakingWater);
            lastLeakingTime = now;
        }

        if (water < capacity) {
            water++;
            return true;
        } else {
            return false;
        }
    }
}
