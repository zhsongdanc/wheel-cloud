package com.wheel.cloud.task.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskTypeConfig {

    private Long id;
    private String taskTypeCode;
    private String taskTypeName;
    private String executorType;
    private Integer maxRetryCount;
    private Integer retryIntervalSeconds;

    /** FIXED 或 EXPONENTIAL_BACKOFF */
    private String retryStrategy;

    private Integer timeoutSeconds;
    private Integer enabled;
    private Integer priority;
    private Integer defaultExpireSeconds;

    /** 0-不复用会话，1-复用 */
    private Integer useSession;

    private String description;
    private String rateLimitKey;
    private String rateLimitEvents;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
