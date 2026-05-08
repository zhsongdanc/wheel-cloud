package com.wheel.cloud.task.constant;

import java.util.*;

public class TaskStatusTransitionRules {

    private TaskStatusTransitionRules() {}

    private static final Map<TaskStatusEnum, Set<TaskStatusEnum>> RULES;

    static {
        Map<TaskStatusEnum, Set<TaskStatusEnum>> m = new HashMap<>();
        m.put(TaskStatusEnum.PENDING,
                unmodifiable(TaskStatusEnum.RUNNING, TaskStatusEnum.CANCELLED, TaskStatusEnum.FAILED));
        m.put(TaskStatusEnum.RUNNING,
                unmodifiable(TaskStatusEnum.COMPLETED, TaskStatusEnum.FAILED, TaskStatusEnum.RETRYING,
                        TaskStatusEnum.PAUSED, TaskStatusEnum.CANCELLED, TaskStatusEnum.WAITING_SUB_TASK));
        m.put(TaskStatusEnum.PAUSED,
                unmodifiable(TaskStatusEnum.PENDING, TaskStatusEnum.CANCELLED, TaskStatusEnum.FAILED));
        m.put(TaskStatusEnum.RETRYING,
                unmodifiable(TaskStatusEnum.PENDING, TaskStatusEnum.FAILED, TaskStatusEnum.CANCELLED));
        m.put(TaskStatusEnum.WAITING_SUB_TASK,
                unmodifiable(TaskStatusEnum.PENDING, TaskStatusEnum.FAILED, TaskStatusEnum.CANCELLED));
        m.put(TaskStatusEnum.COMPLETED, Collections.emptySet());
        m.put(TaskStatusEnum.FAILED,
                unmodifiable(TaskStatusEnum.PENDING, TaskStatusEnum.WAITING_SUB_TASK));
        m.put(TaskStatusEnum.CANCELLED,
                unmodifiable(TaskStatusEnum.PENDING));
        RULES = Collections.unmodifiableMap(m);
    }

    private static Set<TaskStatusEnum> unmodifiable(TaskStatusEnum... statuses) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(statuses)));
    }

    public static Set<TaskStatusEnum> getValidTransitions(TaskStatusEnum from) {
        return RULES.getOrDefault(from, Collections.emptySet());
    }

    public static boolean canTransition(TaskStatusEnum from, TaskStatusEnum to) {
        if (from == null || to == null) return false;
        return getValidTransitions(from).contains(to);
    }

    public static boolean isFinalStatus(TaskStatusEnum status) {
        return status != null && status.isFinalStatus();
    }
}
