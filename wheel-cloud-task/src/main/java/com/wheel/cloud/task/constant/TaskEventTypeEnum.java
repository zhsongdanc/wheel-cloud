package com.wheel.cloud.task.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum TaskEventTypeEnum {

    TASK_CREATED_EVENT("TaskCreatedEvent", "任务创建",
            TaskStatusEnum.PENDING),
    TASK_STARTED_EVENT("TaskStartedEvent", "任务开始执行",
            TaskStatusEnum.RUNNING),
    TASK_COMPLETED_EVENT("TaskCompletedEvent", "任务完成",
            TaskStatusEnum.COMPLETED),
    TASK_FAILED_EVENT("TaskFailedEvent", "任务失败",
            TaskStatusEnum.FAILED),
    TASK_RETRYING_EVENT("TaskRetryingEvent", "任务重试",
            TaskStatusEnum.RETRYING, TaskStatusEnum.PENDING),
    TASK_CANCELLED_EVENT("TaskCancelledEvent", "任务取消",
            TaskStatusEnum.CANCELLED),
    TASK_PAUSED_EVENT("TaskPausedEvent", "任务暂停",
            TaskStatusEnum.PAUSED),
    TASK_RESUMED_EVENT("TaskResumedEvent", "任务恢复",
            TaskStatusEnum.RUNNING),
    TASK_WAITING_SUB_TASKS_EVENT("TaskWaitingSubTasksEvent", "任务等待子任务",
            TaskStatusEnum.WAITING_SUB_TASK),
    SUB_TASKS_COMPLETED_EVENT("SubTasksCompletedEvent", "子任务完成",
            TaskStatusEnum.COMPLETED),
    TASK_MANUAL_RETRIED_EVENT("TaskManualRetriedEvent", "任务手动重试",
            TaskStatusEnum.PENDING),
    TASK_STATUS_CHANGED_EVENT("TaskStatusChangedEvent", "任务状态变更",
            TaskStatusEnum.PENDING, TaskStatusEnum.RUNNING, TaskStatusEnum.PAUSED,
            TaskStatusEnum.RETRYING, TaskStatusEnum.WAITING_SUB_TASK,
            TaskStatusEnum.COMPLETED, TaskStatusEnum.FAILED, TaskStatusEnum.CANCELLED);

    private final String eventType;
    private final String description;
    private final Set<TaskStatusEnum> possibleStatuses;

    TaskEventTypeEnum(String eventType, String description, TaskStatusEnum... statuses) {
        this.eventType = eventType;
        this.description = description;
        this.possibleStatuses = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(statuses)));
    }

    public String getEventType() { return eventType; }
    public String getDescription() { return description; }
    public Set<TaskStatusEnum> getPossibleStatuses() { return possibleStatuses; }

    public boolean canLeadToStatus(TaskStatusEnum target) {
        return possibleStatuses.contains(target);
    }

    public static TaskEventTypeEnum fromEventType(String eventType) {
        if (eventType == null) throw new IllegalArgumentException("Event type cannot be null");
        for (TaskEventTypeEnum e : values()) {
            if (e.eventType.equals(eventType)) return e;
        }
        throw new IllegalArgumentException("Unknown event type: " + eventType);
    }
}
