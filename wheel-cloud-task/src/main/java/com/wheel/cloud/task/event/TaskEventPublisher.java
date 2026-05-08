package com.wheel.cloud.task.event;

import com.wheel.cloud.task.constant.TaskEventTypeEnum;
import com.wheel.cloud.task.constant.TaskStatusEnum;
import com.wheel.cloud.task.entity.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 任务事件发布器，用 Spring ApplicationEventPublisher 替代原来的 Mafka
 *
 * 下游可通过 @EventListener 或 @TransactionalEventListener 监听 TaskEvent
 */
@Component
public class TaskEventPublisher {

    @Autowired
    private ApplicationEventPublisher publisher;

    public void publishTaskCreatedEvent(Task task) {
        publish(TaskEventTypeEnum.TASK_CREATED_EVENT, task, null, "SYSTEM");
    }

    public void publishTaskCompletedEvent(Task task) {
        publish(TaskEventTypeEnum.TASK_COMPLETED_EVENT, task, TaskStatusEnum.RUNNING, "SYSTEM");
    }

    public void publishTaskFailedEvent(Task task) {
        publish(TaskEventTypeEnum.TASK_FAILED_EVENT, task, null, "SYSTEM");
    }

    public void publishTaskRetryingEvent(Task task) {
        publish(TaskEventTypeEnum.TASK_RETRYING_EVENT, task, null, "SYSTEM");
    }

    public void publishTaskCancelledEvent(Task task) {
        publish(TaskEventTypeEnum.TASK_CANCELLED_EVENT, task, null, "SYSTEM");
    }

    public void publishTaskPausedEvent(Task task, TaskStatusEnum fromStatus) {
        publish(TaskEventTypeEnum.TASK_PAUSED_EVENT, task, fromStatus, "SYSTEM");
    }

    public void publishTaskResumedEvent(Task task) {
        publish(TaskEventTypeEnum.TASK_RESUMED_EVENT, task, TaskStatusEnum.PAUSED, "SYSTEM");
    }

    public void publishTaskWaitingSubTasksEvent(Task task, int subTaskCount) {
        publish(TaskEventTypeEnum.TASK_WAITING_SUB_TASKS_EVENT, task, null, "SYSTEM");
    }

    public void publishTaskManualRetriedEvent(Task task, TaskStatusEnum fromStatus, String operator) {
        publish(TaskEventTypeEnum.TASK_MANUAL_RETRIED_EVENT, task, fromStatus,
                operator != null ? operator : "SYSTEM");
    }

    private void publish(TaskEventTypeEnum eventType, Task task,
                         TaskStatusEnum fromStatus, String operator) {
        publisher.publishEvent(new TaskEvent(this, eventType, task, fromStatus, operator));
    }
}
