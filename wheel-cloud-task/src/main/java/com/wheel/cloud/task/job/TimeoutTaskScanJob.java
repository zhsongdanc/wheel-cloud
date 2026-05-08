package com.wheel.cloud.task.job;

import com.wheel.cloud.task.entity.Task;
import com.wheel.cloud.task.entity.TaskTypeConfig;
import com.wheel.cloud.task.repository.TaskRepository;
import com.wheel.cloud.task.repository.TaskTypeConfigRepository;
import com.wheel.cloud.task.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 超时任务扫描 Job
 *
 * 职责：扫描 RUNNING 且超过超时阈值的任务，调用 failTask(TIMEOUT_FAILED) 标记失败。
 *
 * 判断流程：
 * 1. 以系统兜底阈值（fallbackTimeoutMinutes）先扫出候选任务（start_time 过早的 RUNNING 任务）
 * 2. 批量加载候选任务的类型配置，避免循环查库（N+1 问题）
 * 3. 按各任务类型自身的 timeout_seconds 做精确判断
 *
 * 执行频率：每 5 分钟一次
 */
@Slf4j
@Component
public class TimeoutTaskScanJob {

    private static final int DEFAULT_FALLBACK_TIMEOUT_MINUTES = 10;
    private static final int BATCH_SIZE = 100;

    @Value("${task.timeout.scan.fallback-minutes:" + DEFAULT_FALLBACK_TIMEOUT_MINUTES + "}")
    private int fallbackTimeoutMinutes;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskTypeConfigRepository taskTypeConfigRepository;

    @Autowired
    private TaskService taskService;

    @Scheduled(fixedDelay = 5 * 60_000L)
    public void scanTimeoutTasks() {
        try {
            LocalDateTime fallbackThreshold = LocalDateTime.now().minusMinutes(fallbackTimeoutMinutes);
            List<Task> candidates = taskRepository.getAbnormalRunningTasks(fallbackThreshold, BATCH_SIZE);
            if (CollectionUtils.isEmpty(candidates)) {
                return;
            }

            // 批量加载类型配置，避免循环查库
            Set<String> typeCodes = candidates.stream()
                    .map(Task::getTaskTypeCode)
                    .collect(Collectors.toSet());
            Map<String, TaskTypeConfig> configMap = taskTypeConfigRepository.getMapByTaskTypeCodes(typeCodes);

            log.info("TimeoutTaskScanJob 扫出 {} 个候选任务，涉及 {} 种类型，开始精确判断",
                    candidates.size(), configMap.size());

            LocalDateTime now = LocalDateTime.now();
            for (Task task : candidates) {
                handleTimeoutTask(task, configMap, now);
            }
        } catch (Exception e) {
            log.error("TimeoutTaskScanJob 执行失败", e);
        }
    }

    private void handleTimeoutTask(Task task, Map<String, TaskTypeConfig> configMap,
                                   LocalDateTime now) {
        try {
            if (task.getStartTime() == null) {
                log.warn("任务 {} startTime 为空，跳过超时判断", task.getId());
                return;
            }

            long elapsedSeconds = java.time.Duration.between(task.getStartTime(), now).getSeconds();
            long timeoutSeconds = resolveTimeoutSeconds(configMap.get(task.getTaskTypeCode()));

            if (elapsedSeconds < timeoutSeconds) {
                return;
            }

            long elapsedMinutes = elapsedSeconds / 60;
            log.warn("任务 {} (type={}) 已执行 {} 分钟，超过超时阈值 {} 秒，标记 TIMEOUT_FAILED",
                    task.getId(), task.getTaskTypeCode(), elapsedMinutes, timeoutSeconds);

            taskService.failTask(task.getId(), "TIMEOUT_FAILED",
                    "任务执行超时，已运行 " + elapsedMinutes + " 分钟，配置超时 " + timeoutSeconds + " 秒");
        } catch (Exception e) {
            log.error("处理超时任务失败, taskId={}", task.getId(), e);
        }
    }

    /** 优先使用类型配置的 timeout_seconds；缺省时使用兜底阈值 */
    private long resolveTimeoutSeconds(TaskTypeConfig config) {
        if (config != null && config.getTimeoutSeconds() != null && config.getTimeoutSeconds() > 0) {
            return config.getTimeoutSeconds();
        }
        return (long) fallbackTimeoutMinutes * 60;
    }
}
