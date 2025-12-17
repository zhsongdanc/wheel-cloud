package com.wheel.cloud.hystrix.analytics;

import lombok.Data;

import java.util.concurrent.atomic.LongAdder;

@Data
public class BucketInfo {
    private long startTime;
    private LongAdder totalRequestCount = new LongAdder();
    private LongAdder failedRequestCount = new LongAdder();
    private LongAdder successRequestCount = new LongAdder();
}
