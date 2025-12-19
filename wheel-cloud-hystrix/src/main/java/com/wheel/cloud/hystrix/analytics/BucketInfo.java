package com.wheel.cloud.hystrix.analytics;

import lombok.Data;

import java.util.concurrent.atomic.LongAdder;

@Data
public class BucketInfo {

    public BucketInfo(long windowDuration) {
        this.windowDuration = windowDuration;

        long now = System.currentTimeMillis();

        this.windowStartTime = now - now % 1000;
    }

    private long windowDuration;

    private long windowStartTime;

    private long startTime = System.currentTimeMillis();
    private LongAdder totalRequestCount = new LongAdder();
    private LongAdder failedRequestCount = new LongAdder();
    private LongAdder successRequestCount = new LongAdder();

}
