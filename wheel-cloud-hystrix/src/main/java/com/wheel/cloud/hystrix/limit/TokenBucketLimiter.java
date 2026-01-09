package com.wheel.cloud.hystrix.limit;

public class TokenBucketLimiter implements RateLimiter{

    private static final long DEFAULT_CAPACITY = 1000;

    private long lastFillTime;

    private long currentToken;

    // 每秒生产桶数
    private long qps;

    private long capacity;

    public TokenBucketLimiter(long qps) {
        this(qps, DEFAULT_CAPACITY);
    }

    public TokenBucketLimiter(long qps, long capacity) {
        this.qps = qps;
        this.lastFillTime = System.currentTimeMillis();
        this.currentToken = 0;
        this.capacity = capacity;
    }


    @Override
    public synchronized boolean allowRequest() {
        fillTokens();
        if (currentToken > 0) {
            currentToken--;
            return true;
        }
        return false;
    }


    private void fillTokens() {
        long now = System.currentTimeMillis();
        long interval = now - lastFillTime;
        currentToken = Math.min(capacity, currentToken + interval * qps / 1000);
        lastFillTime = now;
    }
}
