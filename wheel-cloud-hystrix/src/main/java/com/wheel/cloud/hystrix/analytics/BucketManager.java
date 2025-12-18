package com.wheel.cloud.hystrix.analytics;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 该类需要考虑读写并发问题
 */
public class BucketManager {

    private static final long START_TIME = System.currentTimeMillis();

    private static final int DEFAULT_SIZE = 10;

    private static final int DEFAULT_BUCKET_TIME = 1000;

    private AtomicReferenceArray<BucketInfo> circularBucket = new AtomicReferenceArray<>(DEFAULT_SIZE);

    // 1. 回收

    // 不采用定时回收的理由：（1）使用的内存区域是固定的，不会耗费很多内存 （2）使用定时任务会有延迟问题，依然需要读/写时清理 （3）更新桶和清理桶存在并发问题
    //                   （4）高并发时虽然CPU高，但是必须的；低并发时定时任务无效


    // 2. 统计


    public float getSuccessRate() {
        int totalCount = 0;
        int successCount = 0;
        for (int i = 0; i < circularBucket.length(); i++) {
            BucketInfo bucketInfo = circularBucket.get(i);
            if (bucketInfo == null) {
                continue;
            }
            totalCount += bucketInfo.getTotalRequestCount().sum();
            successCount += bucketInfo.getSuccessRequestCount().sum();
        }

        return totalCount == 0 ? 0 : (float) successCount / totalCount;
    }


    public void clearSingleBucketIfNecessary(long timestamp) {
        int bucketIndex = getBucketIndex(timestamp);
        if (circularBucket.get(bucketIndex) != null && circularBucket.get(bucketIndex).isExpired()) {
            circularBucket.set(bucketIndex, null);
        }
    }

    public void clearAll() {
        for (int i = 0; i < DEFAULT_SIZE; i++) {
            circularBucket.set(i, null);
        }
    }


    public void recordSingle(long timestamp, boolean success) {
        int bucketIndex = getBucketIndex(timestamp);
        BucketInfo bucketInfo = circularBucket.get(bucketIndex);
        if (bucketInfo.isExpired()) {
            circularBucket.compareAndSet(bucketIndex, bucketInfo, new BucketInfo(System.currentTimeMillis()));
        }
        bucketInfo = circularBucket.get(bucketIndex);
        bucketInfo.getTotalRequestCount().increment();
        if (success) {
            bucketInfo.getSuccessRequestCount().increment();
        } else {
            bucketInfo.getFailedRequestCount().increment();
        }

    }


    public BucketInfo getOrCreateBucket(long timestamp) {
        int bucketIndex = getBucketIndex(timestamp);
        BucketInfo bucketInfo = circularBucket.get(bucketIndex);
        if (bucketInfo == null) {
            bucketInfo = new BucketInfo(DEFAULT_SIZE * DEFAULT_BUCKET_TIME / 1000);
            // 并发场景下，只有首个线程可以操作成功
            circularBucket.compareAndSet(bucketIndex, null, bucketInfo);
        }

        return circularBucket.get(bucketIndex);
    }

    private int getBucketIndex(long timestamp) {
        return (int) (timestamp - START_TIME) / DEFAULT_BUCKET_TIME;
    }


    public float computeAndGetSuccessRate() {
        return 0;
    }
}
