package com.wheel.cloud.hystrix.anno;

import com.wheel.cloud.hystrix.property.ThreadPoolProperty;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashMap;
import java.util.Map;

public class CommandParser {


    private static final String CORE_SIZE_KEY = "coreSize";
    private static final String MAX_SIZE_KEY = "maxSize";
    private static final String KEEP_ALIVE_TIME_KEY = "keepAliveTimeSeconds";
    private static final String QUEUE_SIZE_KEY = "queueSize";

    private static final String MAX_SEMAPHORE_KEY = "maxSemaphore";

    private static final int DEFAULT_CORE_SIZE = 10;
    private static final int DEFAULT_MAX_SIZE = 20;
    private static final int DEFAULT_KEEP_ALIVE_SECONDS = 100;
    private static final int DEFAULT_QUEUE_SIZE = 100;

    private static final int DEFAULT_MAX_SEMAPHORE = 20;


    // 未使用，暂不实现
    public static com.wheel.cloud.hystrix.property.HystrixProperty parseHystrixCommand(HystrixCommand hystrixCommand) {
        if (hystrixCommand == null) {
            return null;
        }

        String fallbackMethod = hystrixCommand.fallbackMethod();
        String groupKey = hystrixCommand.groupKey();
        String methodKey = hystrixCommand.methodKey();
        String threadPoolKey = hystrixCommand.threadPoolKey();
        HystrixProperty[] commandProperties = hystrixCommand.commandProperties();
        HystrixProperty[] threadPoolProperties = hystrixCommand.threadPoolProperties();
        return null;
    }


    public static ThreadPoolProperty parseHystrixProperties(HystrixProperty[] hystrixProperties) {
        Map<String, Object> properties = new HashMap<>();
        for (HystrixProperty hystrixProperty : hystrixProperties) {
            properties.put(hystrixProperty.name(), hystrixProperty.value());
        }

        ThreadPoolProperty threadPoolProperty = new ThreadPoolProperty();
        threadPoolProperty.setCoreSize((int)properties.getOrDefault(CORE_SIZE_KEY, DEFAULT_CORE_SIZE));
        threadPoolProperty.setMaxSize((int)properties.getOrDefault(MAX_SIZE_KEY, DEFAULT_MAX_SIZE));
        threadPoolProperty.setKeepAliveTimeSeconds((int)properties.getOrDefault(KEEP_ALIVE_TIME_KEY, DEFAULT_KEEP_ALIVE_SECONDS));
        threadPoolProperty.setQueueSize((int)properties.getOrDefault(QUEUE_SIZE_KEY, DEFAULT_QUEUE_SIZE));
        return threadPoolProperty;
    }

    public static int parseMaxSemaphore(HystrixProperty[] hystrixProperties) {
        for (HystrixProperty hystrixProperty : hystrixProperties) {
            if (hystrixProperty.name().equals(MAX_SEMAPHORE_KEY)) {
                return NumberUtils.toInt(hystrixProperty.value(), DEFAULT_MAX_SEMAPHORE);
            }
        }
        return DEFAULT_MAX_SEMAPHORE;
    }

}
