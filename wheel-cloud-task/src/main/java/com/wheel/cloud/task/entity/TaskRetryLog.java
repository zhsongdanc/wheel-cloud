package com.wheel.cloud.task.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRetryLog {

    private Long id;
    private Long taskId;
    private String sessionId;
    private Integer retryCount;
    private String errorType;
    private String errorCode;
    private String errorMsg;
    private String stackTrace;
    private Integer executeDuration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime nextRetryTime;
    private String retryStrategy;
    private LocalDateTime createTime;
}
