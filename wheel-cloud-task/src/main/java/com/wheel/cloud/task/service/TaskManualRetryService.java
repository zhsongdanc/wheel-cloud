package com.wheel.cloud.task.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wheel.cloud.task.constant.TaskEventTypeEnum;
import com.wheel.cloud.task.constant.TaskStatusEnum;
import com.wheel.cloud.task.entity.Task;
import com.wheel.cloud.task.entity.TaskEvent;
import com.wheel.cloud.task.event.TaskEventPublisher;
import com.wheel.cloud.task.repository.TaskEventRepository;
import com.wheel.cloud.task.repository.TaskRepository;
import com.wheel.cloud.task.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务手动重试服务
 *
 * 职责：
 * - 将 FAILED/CANCELLED 任务重置为 PENDING
 * - 子任务重试时联动将因子任务失败而 FAILED 的父任务恢复为 WAITING_SUB_TASK
 * - 支持单个和批量重试，批量场景对父任务去重
 *
 * 事务边界：
 * - 单个重试：子任务更新 + 父任务联动在同一事务内原子提交
 * - 批量重试：每个子任务独立事务；父任务去重后每个独立事务
 */
@Slf4j
@Service
public class TaskManualRetryService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskEventRepository taskEventRepository;
    @Autowired
    private TaskEventPublisher taskEventPublisher;

    @Autowired
    private ApplicationContext applicationContext;

    private TaskManualRetryService self() {
        return applicationContext.getBean(TaskManualRetryService.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public void manualRetryTask(Long taskId, String operator) {
        Task task = requireTask(taskId);
        TaskStatusEnum fromStatus = TaskStatusEnum.fromCode(task.getStatus());
        if (!fromStatus.isManualRetryable()) {
            throw new IllegalStateException(
                    "只有 FAILED 或 CANCELLED 状态的任务才能手动重试，当前状态：" + fromStatus);
        }

        Task parentToReset = resolveParentTaskToReset(task);
        resetTaskState(task, parentToReset);

        taskRepository.updateTaskIncludeNull(task);
        recordEvent(task.getId(), fromStatus.getCode(),
                TaskStatusEnum.PENDING.getCode(), TaskEventTypeEnum.TASK_MANUAL_RETRIED_EVENT);

        if (parentToReset != null) {
            taskRepository.updateTaskIncludeNull(parentToReset);
            recordEvent(parentToReset.getId(), TaskStatusEnum.FAILED.getCode(),
                    TaskStatusEnum.WAITING_SUB_TASK.getCode(), TaskEventTypeEnum.TASK_STATUS_CHANGED_EVENT);
            log.info("父任务 {} 因子任务 {} 手动重试，从 FAILED 恢复到 WAITING_SUB_TASK，新轮次={}",
                    parentToReset.getId(), taskId, parentToReset.getExecutionRound());
        }

        taskEventPublisher.publishTaskManualRetriedEvent(task, fromStatus, operator);
        log.info("任务手动重试成功: taskId={}, fromStatus={}, operator={}", taskId, fromStatus, operator);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<Long, String> batchManualRetry(List<Long> taskIds, String operator) {
        log.info("批量手动重试任务: taskIds={}, operator={}", taskIds, operator);
        Map<Long, String> results = new HashMap<>();
        if (CollectionUtils.isEmpty(taskIds)) return results;

        Map<Long, Task> parentTasksToReset = new HashMap<>();

        for (Long taskId : taskIds) {
            try {
                Task parentTask = self().retrySingleWithoutParentReset(taskId, operator);
                if (parentTask != null) {
                    parentTasksToReset.putIfAbsent(parentTask.getId(), parentTask);
                }
                results.put(taskId, "SUCCESS");
            } catch (Exception e) {
                log.error("批量手动重试任务失败: taskId={}", taskId, e);
                results.put(taskId, "FAILED: " + e.getMessage());
            }
        }

        for (Task parentTask : parentTasksToReset.values()) {
            try {
                self().resetParentTaskToWaiting(parentTask);
                log.info("批量重试：父任务 {} 联动恢复为 WAITING_SUB_TASK", parentTask.getId());
            } catch (Exception e) {
                log.error("批量重试时联动重置父任务失败, parentTaskId={}", parentTask.getId(), e);
            }
        }

        long successCount = results.values().stream().filter("SUCCESS"::equals).count();
        log.info("批量手动重试完成: 总数={}, 成功={}, 失败={}",
                taskIds.size(), successCount, taskIds.size() - successCount);
        return results;
    }

    @Transactional(rollbackFor = Exception.class)
    public Task retrySingleWithoutParentReset(Long taskId, String operator) {
        Task task = requireTask(taskId);
        TaskStatusEnum fromStatus = TaskStatusEnum.fromCode(task.getStatus());
        if (!fromStatus.isManualRetryable()) {
            throw new IllegalStateException(
                    "只有 FAILED 或 CANCELLED 状态的任务才能手动重试，当前：" + fromStatus);
        }

        Task parentToReset = resolveParentTaskToReset(task);
        resetTaskState(task, parentToReset);

        taskRepository.updateTaskIncludeNull(task);
        recordEvent(task.getId(), fromStatus.getCode(),
                TaskStatusEnum.PENDING.getCode(), TaskEventTypeEnum.TASK_MANUAL_RETRIED_EVENT);

        taskEventPublisher.publishTaskManualRetriedEvent(task, fromStatus, operator);
        log.info("任务手动重试成功: taskId={}, fromStatus={}", taskId, fromStatus);
        return parentToReset;
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetParentTaskToWaiting(Task parentTask) {
        taskRepository.updateTaskIncludeNull(parentTask);
        recordEvent(parentTask.getId(), TaskStatusEnum.FAILED.getCode(),
                TaskStatusEnum.WAITING_SUB_TASK.getCode(), TaskEventTypeEnum.TASK_STATUS_CHANGED_EVENT);
    }

    // ======================== 私有方法 ========================

    /**
     * 重置任务字段，清空上一轮执行痕迹
     *
     * expireTime/startTime 必须清空：旧值会导致下一轮立即被判定为超时/已启动
     */
    private void resetTaskState(Task task, Task parentTask) {
        task.setStatus(TaskStatusEnum.PENDING.getCode());
        task.setSubStatus(null);
        task.setFailureReason(null);
        task.setErrorMsg(null);
        task.setStartTime(null);
        task.setEndTime(null);
        task.setExpireTime(null);
        task.setRetryCount(0);
        task.setSessionId(null);
        task.setScheduledTime(LocalDateTime.now());
        task.setExecutionRound(task.getExecutionRound() != null ? task.getExecutionRound() + 1 : 1);
        if (parentTask != null) {
            task.setParentExecutionRound(parentTask.getExecutionRound());
        }
        task.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 判断是否需要联动重置父任务
     * 条件：有父任务 && 父任务 FAILED && subStatus = SUB_TASK_FAILED
     * 若满足，返回已设置好目标状态的父任务；否则返回 null
     */
    private Task resolveParentTaskToReset(Task task) {
        if (task.getParentId() == null) return null;
        Task parent = taskRepository.getTaskById(task.getParentId());
        if (parent == null) {
            log.warn("子任务 {} 的父任务 {} 不存在，跳过联动", task.getId(), task.getParentId());
            return null;
        }
        if (!TaskStatusEnum.FAILED.getCode().equals(parent.getStatus())
                || !"SUB_TASK_FAILED".equals(parent.getSubStatus())) {
            log.info("父任务 {} 状态={}(subStatus={})，不满足联动条件，跳过",
                    parent.getId(), parent.getStatus(), parent.getSubStatus());
            return null;
        }

        parent.setStatus(TaskStatusEnum.WAITING_SUB_TASK.getCode());
        parent.setSubStatus(null);
        parent.setFailureReason(null);
        parent.setErrorMsg(null);
        parent.setEndTime(null);
        parent.setExpireTime(null);
        int oldRound = parent.getExecutionRound() != null ? parent.getExecutionRound() : 1;
        int newRound = oldRound + 1;
        parent.setExecutionRound(newRound);
        parent.setWakeupContext(copyLastRoundContext(parent.getWakeupContext(), oldRound, newRound));
        parent.setUpdateTime(LocalDateTime.now());
        log.info("子任务 {} 手动重试，联动恢复父任务 {} 为 WAITING_SUB_TASK", task.getId(), parent.getId());
        return parent;
    }

    /**
     * 将 oldRound 对应的 wakeupContext 复制到 newRound，保证父任务新轮次恢复时能读到正确上下文
     */
    private String copyLastRoundContext(String wakeupContext, int oldRound, int newRound) {
        Map<String, String> ctx = new HashMap<>();
        if (StringUtils.isNotBlank(wakeupContext)) {
            try {
                Map<String, String> parsed = JsonUtil.parseObject(
                        wakeupContext, new TypeReference<Map<String, String>>() {});
                if (parsed != null) ctx.putAll(parsed);
            } catch (Exception e) {
                log.warn("解析 wakeupContext 失败，以空上下文追加新轮次, wakeupContext={}", wakeupContext, e);
            }
        }
        String last = ctx.get(String.valueOf(oldRound));
        if (last == null) last = ctx.get(String.valueOf(oldRound - 1));
        ctx.put(String.valueOf(newRound), StringUtils.defaultString(last));
        return JsonUtil.toJson(ctx);
    }

    private Task requireTask(Long taskId) {
        if (taskId == null) throw new IllegalArgumentException("任务ID不能为空");
        Task task = taskRepository.getTaskById(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在: " + taskId);
        return task;
    }

    private void recordEvent(Long taskId, String fromStatus, String toStatus,
                             TaskEventTypeEnum eventType) {
        TaskEvent event = new TaskEvent();
        event.setTaskId(taskId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setEventType(eventType.getEventType());
        event.setOperator("SYSTEM");
        event.setEventTime(LocalDateTime.now());
        event.setCreateTime(LocalDateTime.now());
        taskEventRepository.saveTaskEvent(event);
    }
}
