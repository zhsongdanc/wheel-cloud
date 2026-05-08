package com.wheel.cloud.task.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskEvent {

    private Long id;
    private Long taskId;
    private String eventType;
    private String fromStatus;
    private String toStatus;
    private Map<String, Object> eventData;
    private String operator;
    private LocalDateTime eventTime;
    private LocalDateTime createTime;
}
