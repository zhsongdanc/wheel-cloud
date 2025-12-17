package com.wheel.cloud.hystrix.analytics;

import java.util.concurrent.atomic.AtomicReferenceArray;

public class CircularList {

    private static final int DEFAULT_SIZE = 10;

    private AtomicReferenceArray<BucketInfo> circularBucket = new AtomicReferenceArray<>(DEFAULT_SIZE);

    // 1. 回收

    // 2. 统计


    public float getSuccessRate() {
        return 1.0f;
    }


    // todo resetBucket&getOrCreateBucket的并发问题
    public void resetBucket(int buckedIndex) {

    }


    public void record(long startTime, boolean success) {

    }


    public BucketInfo getOrCreateBucket(long startTime) {
        return null;
    }

    public void clearExpiredBucket() {
    }
}
