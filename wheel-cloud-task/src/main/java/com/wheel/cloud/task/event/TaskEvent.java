package com.wheel.cloud.task.event;

import com.wheel.cloud.task.constant.TaskEventTypeEnum;
import com.wheel.cloud.task.constant.TaskStatusEnum;
import org.springframework.context.ApplicationEvent;

/**
 * 任务状态变更 Spring 事件，替代原来的 Mafka 消息
 */
public class TaskEvent extends ApplicationEvent {

    private final TaskEventTypeEnum eventType;
    private final com.wheel.cloud.task.entity.Task task;
    private final TaskStatusEnum fromStatus;
    private final String operator;

    public TaskEvent(Object source, TaskEventTypeEnum eventType,
                     com.wheel.cloud.task.entity.Task task,
                     TaskStatusEnum fromStatus, String operator) {
        super(source);
        this.eventType = eventType;
        this.task = task;
        this.fromStatus = fromStatus;
        this.operator = operator;
    }

    public TaskEventTypeEnum getEventType() { return eventType; }
    public com.wheel.cloud.task.entity.Task getTask() { return task; }
    public TaskStatusEnum getFromStatus() { return fromStatus; }
    public String getOperator() { return operator; }
}
