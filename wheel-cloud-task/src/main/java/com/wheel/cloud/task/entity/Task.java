package com.wheel.cloud.task.entity;

import com.wheel.cloud.task.constant.WaitStrategyEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Task {

    private Long id;
    private String taskBizId;
    private String taskName;
    private String taskTypeCode;
    private String executorType;
    private WaitStrategyEnum waitStrategy;
    private String creator;
    private String assignee;
    private Integer priority;
    private String status;
    private String subStatus;
    private String failureReason;
    private String sessionId;
    private Integer version;
    private Long parentId;

    /** 父任务创建本子任务时的 executionRound，用于区分多轮子任务 */
    private Integer parentExecutionRound;

    private Long rootId;
    private Integer executionRound;
    private String inputData;
    private String outputData;

    /**
     * 唤醒上下文 JSON（Map 结构，key=executionRound）
     * 任务从 WAITING_SUB_TASK/PAUSED 恢复时携带，供执行器读取
     */
    private String wakeupContext;

    private LocalDateTime scheduledTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime expireTime;
    private Integer retryCount;
    private Integer maxRetry;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
