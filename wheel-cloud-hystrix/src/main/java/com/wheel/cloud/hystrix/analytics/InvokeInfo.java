package com.wheel.cloud.hystrix.analytics;

import lombok.Builder;
import lombok.Data;

/**
 * v1.0版本遗留类
 */
@Builder
@Data
public class InvokeInfo {
    private String methodKey;
    private boolean success;
    private long startTime;
    private long duration;
    private String exceptionName;
    private String exceptionMessage;
}
