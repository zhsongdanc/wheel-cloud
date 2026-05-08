package com.wheel.cloud.task.constant;

import java.util.Set;

public enum TaskStatusEnum {

    PENDING("PENDING", "待执行", 1),
    RUNNING("RUNNING", "执行中", 2),
    PAUSED("PAUSED", "已暂停", 3),
    RETRYING("RETRYING", "重试中", 4),
    WAITING_SUB_TASK("WAITING_SUB_TASK", "等待子任务", 5),
    COMPLETED("COMPLETED", "已完成", 6),
    FAILED("FAILED", "执行失败", 7),
    CANCELLED("CANCELLED", "已取消", 8);

    private final String code;
    private final String description;
    private final int order;

    TaskStatusEnum(String code, String description, int order) {
        this.code = code;
        this.description = description;
        this.order = order;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
    public int getOrder() { return order; }

    public static TaskStatusEnum fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Task status code cannot be null");
        }
        for (TaskStatusEnum s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("Unknown task status: " + code);
    }

    public boolean isFinalStatus() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean isManualRetryable() {
        return this == FAILED || this == CANCELLED;
    }

    public Set<TaskStatusEnum> getValidTransitions() {
        return TaskStatusTransitionRules.getValidTransitions(this);
    }

    public boolean canTransitionTo(TaskStatusEnum target) {
        return getValidTransitions().contains(target);
    }
}
