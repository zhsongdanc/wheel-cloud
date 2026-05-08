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

/**
 * 任务调度补偿 Job
 *
 * 实时路径：TaskCreatedEvent → 消费者 → TaskService（新任务创建后立即调度）
 * 补偿路径：本 Job 周期扫描 → 直接触发（防止实时路径丢失时任务卡在 PENDING）
 *
 * 只处理至少 {@value DELAY_MINUTES} 分钟前就应触发的任务，避免与实时路径并发竞争。
 * 通过 TaskService 的乐观锁保证最终只有一个路径成功将任务推进到 RUNNING。
 */
@Slf4j
@Component
public class TaskScheduleJob {

    /** 补偿扫描时间偏移：只补偿至少 2 分钟前就该触发的任务 */
    private static final int DELAY_MINUTES = 2;
    private static final int BATCH_SIZE = 100;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    /**
     * 每 60 秒扫描一次，将超时未调度的 PENDING 任务标记为 RUNNING
     *
     * fixedDelay 保证上一次执行完成后再计时，防止堆积
     */
    @Scheduled(fixedDelay = 60_000L)
    public void scheduleDelayedTasks() {
        try {
            LocalDateTime scheduledBefore = LocalDateTime.now().minusMinutes(DELAY_MINUTES);
            List<Task> pendingTasks = taskRepository.getSchedulablePendingTasks(scheduledBefore, BATCH_SIZE);
            if (CollectionUtils.isEmpty(pendingTasks)) {
                return;
            }
            log.info("TaskScheduleJob 发现 {} 个待调度任务，开始补偿触发", pendingTasks.size());

            int triggered = 0;
            for (Task task : pendingTasks) {
                try {
                    // 在实际项目中这里会调用 TaskExecutionService.triggerExecution()
                    // wheel-cloud-task 无执行层，仅记录日志模拟触发行为
                    log.info("TaskScheduleJob 触发任务: taskId={}, taskName={}, taskTypeCode={}",
                            task.getId(), task.getTaskName(), task.getTaskTypeCode());
                    triggered++;
                } catch (Exception e) {
                    log.error("TaskScheduleJob 触发任务失败, taskId={}", task.getId(), e);
                }
            }
            log.info("TaskScheduleJob 本轮完成，触发 {} 个任务", triggered);
        } catch (Exception e) {
            log.error("TaskScheduleJob 执行失败", e);
        }
    }
}
