package com.wheel.cloud.hystrix.limit;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAdder;

public class SlidingWindowLimiter implements RateLimiter {

    private static final int DEFAULT_SMALL_WINDOW_CNT = 10;
    private static final int DEFAULT_INTERVAL_IN_MS = 1000;

    private long qps;

    // 单位：毫秒
    private long singleWindowWidth;

    private int smallWindowCnt;

    private AtomicReferenceArray<BucketCounter> bucketCounters;

    final Object updateLock = new Object();

    public SlidingWindowLimiter(long qps) {
        this(DEFAULT_INTERVAL_IN_MS, DEFAULT_SMALL_WINDOW_CNT, qps);
    }

    public SlidingWindowLimiter(int allIntervalInMs, int smallWindowCnt, long qps) {
        this.singleWindowWidth = allIntervalInMs/smallWindowCnt;
        this.smallWindowCnt = smallWindowCnt;
        this.bucketCounters = new AtomicReferenceArray<>(smallWindowCnt);
        this.qps = qps;
    }


    @Override
    public boolean allowRequest() {
        long now = System.currentTimeMillis();
        long total = 0;

        for (int i = 0; i < smallWindowCnt; i++) {
            BucketCounter counter = bucketCounters.get(i);
            if (counter == null || counter.getStartTime() + singleWindowWidth*smallWindowCnt < now) {
                continue;
            }
            total+=counter.getCounter().sum();
        }
        if (total <= qps) {
            getBucketCounter(now).getCounter().increment();
            return true;
        }
        return false;
    }


    private BucketCounter getBucketCounter(long timestamp) {
        int currentWindowIndex = computeWindowIndex(timestamp);
        long currentStartTimestamp = timestamp - timestamp % singleWindowWidth;
        while (true) {
            BucketCounter bucketCounter = bucketCounters.get(currentWindowIndex);
            if (bucketCounter == null) {
                bucketCounter = new BucketCounter(currentStartTimestamp, new LongAdder());
                if (bucketCounters.compareAndSet(currentWindowIndex, null, bucketCounter)) {
                    return bucketCounter;
                }

            } else if (bucketCounter.getStartTime() < currentStartTimestamp) {

                synchronized (updateLock) {
                    bucketCounter = bucketCounters.get(currentWindowIndex);
                    if (bucketCounter.getStartTime() < currentStartTimestamp) {
                        bucketCounter.getCounter().reset();
                        bucketCounter.setStartTime(currentStartTimestamp);
                    }
                }

                return bucketCounter;
            } else if (bucketCounter.getStartTime() == currentStartTimestamp) {
                return bucketCounter;

            } else {
                // 正常不会出现
                return bucketCounter;
            }
        }

    }


    private int computeWindowIndex(long timestamp) {
        return (int)(timestamp/ singleWindowWidth) % smallWindowCnt;
    }
}
