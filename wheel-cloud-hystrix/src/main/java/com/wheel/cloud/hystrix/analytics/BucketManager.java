package com.wheel.cloud.hystrix.analytics;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 该类需要考虑读写并发问题
 */
public class BucketManager {

    private static final long START_TIME = System.currentTimeMillis();

    private static final int DEFAULT_SIZE = 10;

    private static final int DEFAULT_WINDOW_DURATION = 1000;

    private AtomicReferenceArray<BucketInfo> circularBucket = new AtomicReferenceArray<>(DEFAULT_SIZE);


    // 不采用定时回收的理由：（1）使用的内存区域是固定的，不会耗费很多内存 （2）使用定时任务会有延迟问题，依然需要读/写时清理 （3）更新桶和清理桶存在并发问题
    //                   （4）高并发时虽然CPU高，但是必须的；低并发时定时任务无效


    public void clearAll() {
        for (int i = 0; i < DEFAULT_SIZE; i++) {
            circularBucket.set(i, null);
        }
    }


    public void recordSingle(long timestamp, boolean success) {
        BucketInfo bucketInfo = getBucketInfo(timestamp);
        bucketInfo.getTotalRequestCount().increment();
        if (success) {
            bucketInfo.getSuccessRequestCount().increment();
        } else {
            bucketInfo.getFailedRequestCount().increment();
        }
    }



    public BucketInfo getBucketInfo(long timestamp) {
        int bucketIndex = getBucketIndex(timestamp);
        while (true) {
            BucketInfo oldBucketInfo = circularBucket.get(bucketIndex);
            if (oldBucketInfo == null) {
                BucketInfo newBucket = new BucketInfo(DEFAULT_WINDOW_DURATION);
                boolean compareAndSet = circularBucket.compareAndSet(bucketIndex, oldBucketInfo, newBucket);
                if(compareAndSet){
                    return newBucket;
                }
                continue;
            }

            long shouldWindowStartTime = timestamp - (timestamp % DEFAULT_WINDOW_DURATION);
            if (oldBucketInfo.getWindowStartTime() < shouldWindowStartTime) {
                BucketInfo newBucket = new BucketInfo(DEFAULT_WINDOW_DURATION);
                boolean compareAndSet = circularBucket.compareAndSet(bucketIndex, oldBucketInfo, newBucket);
                if(compareAndSet){
                    return newBucket;
                }
            } else {
                return oldBucketInfo;
            }
        }
    }


    private int getBucketIndex(long timestamp) {
        // 先计算从开始到现在经过了多少个“桶的时间单位”
        long bucketId = (timestamp - START_TIME) / DEFAULT_WINDOW_DURATION;
        // 再对数组长度取模
        return (int) (bucketId % DEFAULT_SIZE);
    }


    public float computeAndGetSuccessRate(long startTime) {
        int totalCount = 0;
        int successCount = 0;

        long startTimeThreshold = startTime - (DEFAULT_SIZE * DEFAULT_WINDOW_DURATION);
        for (int i = 0; i < circularBucket.length(); i++) {
            BucketInfo bucketInfo = circularBucket.get(i);
            if (bucketInfo == null || bucketInfo.getWindowStartTime() < startTimeThreshold) {
                continue;
            }
            totalCount += bucketInfo.getTotalRequestCount().sum();
            successCount += bucketInfo.getSuccessRequestCount().sum();
        }

        return totalCount == 0 ? 0 : (float) successCount / totalCount;
    }

    /*
     统计所有桶中的总请求数,只作为测试接口
     */
    public long getTotalRequestInWindow() {
        long total = 0;
        for (int i = 0; i < circularBucket.length(); i++) {
            BucketInfo bucketInfo = circularBucket.get(i);
            if (bucketInfo == null) {
                continue;
            }
            total += bucketInfo.getTotalRequestCount().sum();
        }
        return total;
    }
}
