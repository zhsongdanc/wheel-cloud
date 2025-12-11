package com.wheel.cloud.hystrix.config;

import lombok.Builder;
import lombok.Data;

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
