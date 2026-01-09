package com.wheel.cloud.hystrix.limit;

import java.util.concurrent.atomic.AtomicReference;

public class TokenBucketLimiterV2 implements RateLimiter{
    private static final long DEFAULT_CAPACITY = 1000;

    private AtomicReference<State> state;

    // 每秒生产桶数
    private long qps;

    private long capacity;

    public TokenBucketLimiterV2(long qps) {
        this(qps, DEFAULT_CAPACITY);
    }

    public TokenBucketLimiterV2(long qps, long capacity) {
        this.qps = qps;
        this.state = new AtomicReference<>(new State(System.currentTimeMillis(), 0));
        this.capacity = capacity;
    }


    @Override
    public boolean allowRequest() {
        while (true) {
            State oldState = state.get();
            long now = System.currentTimeMillis();
            long currentToken = oldState.getCurrentToken();
            long lastFillTime = oldState.getLastFillTime();

            long expectedToken = getExpectedToken(now, lastFillTime, currentToken) - 1;
            if (expectedToken >= 0) {
                if (state.compareAndSet(oldState, new State(now, expectedToken))) {
                    return true;
                }
            } else {
                return false;
            }
        }

    }

    private long getExpectedToken(long now, long lastFillTime, long currentToken) {
        long interval = now - lastFillTime;
        return Math.min(capacity, currentToken + interval * qps / 1000);
    }

    private static class State {
        final long lastFillTime;
        final long currentToken;

        private State(long lastFillTime, long currentToken) {
            this.lastFillTime = lastFillTime;
            this.currentToken = currentToken;
        }

        public long getLastFillTime() {
            return lastFillTime;
        }

        public long getCurrentToken() {
            return currentToken;
        }
    }
}
