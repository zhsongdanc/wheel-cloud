package com.wheel.cloud.hystrix.property;

import lombok.Data;

@Data
public class ThreadPoolProperty {
    private int coreSize;
    private int maxSize;
    private int queueSize;
    private int keepAliveTimeSeconds;

    private int maxQueueSize;
    private int queueSizeRejectionThreshold;
}
