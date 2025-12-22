package com.wheel.cloud.hystrix.property;

import java.util.HashMap;
import java.util.Map;

public class HystrixProperty {

    private static final String CORE_SIZE = "coreSize";
    private static final String MAX_SIZE = "maxSize";
    private static final String QUEUE_SIZE = "queueSize";
    private static final String KEEP_ALIVE_TIME_MINUTES = "keepAliveTimeMinutes";
    private static final String MAX_QUEUE_SIZE = "maxQueueSize";
//    private static final String QUEUE_SIZE_REJECTION_THRESHOLD = "queueSizeRejectionThreshold";


    private String methodKey;

    private String groupKey;

    private String threadPoolKey;

    private String fallbackMethod;

    private Map<String, Object>  commandProperties = new HashMap<>();

    private Map<String, Object>  threadPoolProperties = new HashMap<>();
}
