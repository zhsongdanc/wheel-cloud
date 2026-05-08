package com.wheel.cloud.task.job;

import com.wheel.cloud.task.constant.TaskStatusEnum;
import com.wheel.cloud.task.entity.Task;
import com.wheel.cloud.task.repository.TaskRepository;
import com.wheel.cloud.task.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 过期任务扫描 Job
 *
 * 职责：扫描 expire_time <= NOW() 且处于非终止状态的任务，
 * 调用 failTask(EXPIRED_FAILED) 标记为失败（EXPIRED_FAILED 不触发自动重试）。
 *
 * 覆盖状态：RUNNING / PAUSED / RETRYING / WAITING_SUB_TASK
 * 注：PENDING 状态下的过期任务也应处理，此处按原项目逻辑对齐
 *
 * 执行频率：每 10 分钟一次
 */
@Slf4j
@Component
public class ExpiredTaskScanJob {

    private static final int BATCH_SIZE = 100;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Scheduled(fixedDelay = 10 * 60_000L)
    public void scanExpiredTasks() {
        try {
            scanExpiredByStatus(TaskStatusEnum.RUNNING);
            scanExpiredByStatus(TaskStatusEnum.PAUSED);
            scanExpiredByStatus(TaskStatusEnum.RETRYING);
            scanExpiredByStatus(TaskStatusEnum.WAITING_SUB_TASK);
        } catch (Exception e) {
            log.error("ExpiredTaskScanJob 执行失败", e);
        }
    }

    private void scanExpiredByStatus(TaskStatusEnum status) {
        try {
            List<Task> tasks = taskRepository.getTasksByStatus(status.getCode(), 0, BATCH_SIZE);
            if (CollectionUtils.isEmpty(tasks)) return;

            LocalDateTime now = LocalDateTime.now();
            List<Task> expiredTasks = tasks.stream()
                    .filter(t -> t.getExpireTime() != null && t.getExpireTime().isBefore(now))
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(expiredTasks)) return;

            log.warn("ExpiredTaskScanJob 发现 {} 个 [{}] 状态的过期任务，开始处理",
                    expiredTasks.size(), status.getCode());

            for (Task task : expiredTasks) {
                handleExpiredTask(task);
            }
        } catch (Exception e) {
            log.error("扫描 {} 状态过期任务失败", status.getCode(), e);
        }
    }

    private void handleExpiredTask(Task task) {
        try {
            String expireMsg = "任务超过 expire_time（" + task.getExpireTime() + "）未完成";
            log.warn("任务 {} 已过期（expireTime={}，状态={}），标记 EXPIRED_FAILED",
                    task.getId(), task.getExpireTime(), task.getStatus());
            // EXPIRED_FAILED 不在 canRetry 的 subStatus 范围内，直接进入 FAILED
            taskService.failTask(task.getId(), "EXPIRED_FAILED", expireMsg);
        } catch (Exception e) {
            log.error("处理过期任务失败, taskId={}", task.getId(), e);
        }
    }
}
