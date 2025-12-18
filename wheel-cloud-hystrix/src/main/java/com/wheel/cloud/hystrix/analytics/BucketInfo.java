package com.wheel.cloud.hystrix.analytics;

import lombok.Data;

import java.util.concurrent.atomic.LongAdder;

@Data
public class BucketInfo {

    public BucketInfo(long bucketMaintainTime) {
        this.bucketMaintainTime = bucketMaintainTime;
    }

    private long bucketMaintainTime;

    private long startTime = System.currentTimeMillis();
    private LongAdder totalRequestCount = new LongAdder();
    private LongAdder failedRequestCount = new LongAdder();
    private LongAdder successRequestCount = new LongAdder();


    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > bucketMaintainTime;
    }
}
