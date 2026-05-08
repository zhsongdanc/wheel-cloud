package com.wheel.cloud.task.job;

import com.wheel.cloud.task.constant.TaskStatusEnum;
import com.wheel.cloud.task.entity.Task;
import com.wheel.cloud.task.event.TaskEventPublisher;
import com.wheel.cloud.task.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 重试任务推进 Job
 *
 * 扫描 RETRYING 且 scheduled_time（即 next_retry_time）<= NOW() 的任务，
 * 将状态推进为 PENDING，发布 TaskResumedEvent 触发重新调度。
 *
 * 原项目使用 Redis 分布式锁防多实例重复推进；
 * wheel-cloud-task 为单机学习模块，去掉分布式锁，保留核心状态转换逻辑。
 */
@Slf4j
@Component
public class RetryTaskPushJob {

    private static final int BATCH_SIZE = 100;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskEventPublisher taskEventPublisher;

    @Scheduled(fixedDelay = 60_000L)
    public void pushRetryTasks() {
        try {
            List<Task> retryReadyTasks = taskRepository.getPendingRetryTasks(BATCH_SIZE);
            if (CollectionUtils.isEmpty(retryReadyTasks)) {
                return;
            }
            log.info("RetryTaskPushJob 发现 {} 个到达重试时间的任务，开始推进", retryReadyTasks.size());

            for (Task task : retryReadyTasks) {
                pushRetryTask(task);
            }
        } catch (Exception e) {
            log.error("RetryTaskPushJob 执行失败", e);
        }
    }

    private void pushRetryTask(Task task) {
        try {
            // 重新查询，幂等检查：已被其他路径推进则跳过
            Task current = taskRepository.getTaskById(task.getId());
            if (current == null || !TaskStatusEnum.RETRYING.getCode().equals(current.getStatus())) {
                log.info("任务 {} 状态已变更为 {}，跳过重试推进",
                        task.getId(), current == null ? "null" : current.getStatus());
                return;
            }

            current.setStatus(TaskStatusEnum.PENDING.getCode());
            current.setSubStatus(null);
            current.setScheduledTime(null);
            current.setUpdateTime(LocalDateTime.now());
            taskRepository.updateTask(current);

            // 发布 resumed 事件，触发下游重新调度
            taskEventPublisher.publishTaskResumedEvent(current);

            log.info("任务 {} ({}) 已从 RETRYING 推进到 PENDING（第 {} 次重试）",
                    current.getId(), current.getTaskName(), current.getRetryCount());
        } catch (Exception e) {
            log.error("推进重试任务失败, taskId={}", task.getId(), e);
        }
    }
}
