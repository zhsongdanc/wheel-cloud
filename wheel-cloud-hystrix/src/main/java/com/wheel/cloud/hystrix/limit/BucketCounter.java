package com.wheel.cloud.hystrix.limit;

import lombok.Data;

import java.util.concurrent.atomic.LongAdder;

@Data
public class BucketCounter {

    private long startTime;

    private LongAdder counter;

    public BucketCounter(long startTime, LongAdder counter) {
        this.startTime = startTime;
        this.counter = counter;
    }
}
