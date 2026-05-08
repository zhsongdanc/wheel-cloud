package com.wheel.cloud.task.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wheel.cloud.task.constant.TaskEventTypeEnum;
import com.wheel.cloud.task.constant.TaskStatusEnum;
import com.wheel.cloud.task.constant.WaitStrategyEnum;
import com.wheel.cloud.task.entity.Task;
import com.wheel.cloud.task.entity.TaskEvent;
import com.wheel.cloud.task.entity.TaskRetryLog;
import com.wheel.cloud.task.entity.TaskTypeConfig;
import com.wheel.cloud.task.event.TaskEventPublisher;
import com.wheel.cloud.task.param.CreateTaskParam;
import com.wheel.cloud.task.param.TaskParamConvertor;
import com.wheel.cloud.task.repository.TaskEventRepository;
import com.wheel.cloud.task.repository.TaskRepository;
import com.wheel.cloud.task.repository.TaskRetryLogRepository;
import com.wheel.cloud.task.repository.TaskTypeConfigRepository;
import com.wheel.cloud.task.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务核心服务
 *
 * 设计要点：
 * - DB 写操作在 @Transactional 方法（do*）内完成
 * - Spring 事件在事务提交后发布，避免消息发布失败回滚 DB
 * - self 自注入：确保同 Bean 内调用 @Transactional 方法时走 AOP 代理
 */
@Slf4j
@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskTypeConfigRepository taskTypeConfigRepository;
    @Autowired
    private TaskEventRepository taskEventRepository;
    @Autowired
    private TaskRetryLogRepository taskRetryLogRepository;
    @Autowired
    private TaskEventPublisher taskEventPublisher;
    @Autowired
    private TaskManualRetryService taskManualRetryService;
    @Autowired
    private TaskParamConvertor taskParamConvertor;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 懒获取自身代理，避免 Spring Boot 2.6+ 默认禁止循环依赖
     * 目的：确保同 Bean 内调用 @Transactional 方法时走 AOP 代理使事务生效
     */
    private TaskService self() {
        return applicationContext.getBean(TaskService.class);
    }

    // ======================== 创建任务 ========================

    /**
     * 创建普通任务（PENDING 状态，立即可调度）
     */
    public Long createTask(CreateTaskParam param) {
        log.info("创建任务: taskName={}, taskTypeCode={}, parentTaskId={}",
                param.getTaskName(), param.getTaskTypeCode(), param.getParentTaskId());
        TaskTypeConfig typeConfig = loadTypeConfig(param.getTaskTypeCode());
        Task task = taskParamConvertor.toTask(param, typeConfig);
        task.setTaskBizId(StringUtils.defaultString(task.getTaskBizId()));
        task.setStatus(TaskStatusEnum.PENDING.getCode());
        if (param.getParentTaskId() != null) {
            setupParentRelation(task, param.getParentTaskId());
        }

        Long taskId = self().doCreateTask(task);
        taskEventPublisher.publishTaskCreatedEvent(task);

        log.info("任务创建成功: taskId={}, taskName={}, taskTypeCode={}",
                taskId, task.getTaskName(), param.getTaskTypeCode());
        return taskId;
    }

    /**
     * 创建「等待子任务」的父任务（初始状态 WAITING_SUB_TASK）
     *
     * 父任务创建后不投入调度，子任务全部完成后自动转为 PENDING 触发执行
     */
    public Long createTaskWaitingSubTasks(CreateTaskParam param, WaitStrategyEnum strategy,
                                          String wakeupContext) {
        log.info("创建等待子任务的父任务: taskName={}, strategy={}", param.getTaskName(), strategy);
        TaskTypeConfig typeConfig = loadTypeConfig(param.getTaskTypeCode());
        Task task = taskParamConvertor.toTask(param, typeConfig);
        task.setTaskBizId(StringUtils.defaultString(task.getTaskBizId()));
        task.setStatus(TaskStatusEnum.WAITING_SUB_TASK.getCode());
        task.setWaitStrategy(strategy != null ? strategy : WaitStrategyEnum.ALL_SUCCESS);
        task.setWakeupContext(mergeWakeupContext(null, 1, wakeupContext));
        if (param.getParentTaskId() != null) {
            setupParentRelation(task, param.getParentTaskId());
        }

        Long taskId = self().doCreateTask(task);
        taskEventPublisher.publishTaskWaitingSubTasksEvent(task, 0);

        log.info("等待子任务的父任务创建成功: taskId={}, strategy={}", taskId, task.getWaitStrategy());
        return taskId;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long doCreateTask(Task task) {
        return taskRepository.createTask(task);
    }

    // ======================== 查询 ========================

    public Task queryTaskByBizId(String taskBizId) {
        if (taskBizId == null) throw new IllegalArgumentException("任务bizId不能为空");
        Task task = taskRepository.getTaskByBizId(taskBizId);
        if (task == null) throw new IllegalArgumentException("任务不存在: " + taskBizId);
        return task;
    }

    public Task queryTaskById(Long taskId) {
        if (taskId == null) throw new IllegalArgumentException("任务ID不能为空");
        Task task = taskRepository.getTaskById(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在: " + taskId);
        return task;
    }

    public List<Task> querySubTasks(Long parentTaskId) {
        return taskRepository.getSubTasks(parentTaskId);
    }

    public List<Task> queryCompletedSubTasks(Long parentTaskId) {
        if (parentTaskId == null) throw new IllegalArgumentException("父任务ID不能为空");
        List<Task> subTasks = taskRepository.getSubTasks(parentTaskId);
        if (CollectionUtils.isEmpty(subTasks)) return Collections.emptyList();
        return subTasks.stream()
                .filter(t -> TaskStatusEnum.COMPLETED.getCode().equals(t.getStatus()))
                .collect(Collectors.toList());
    }

    public List<Task> queryTasksByBizId(String bizId) {
        return taskRepository.queryTasksByBizId(bizId);
    }

    public List<Task> queryTasksByBizIdAndTaskType(String bizId, String taskType) {
        return taskRepository.queryTasksByBizIdAndTaskType(bizId, taskType);
    }

    // ======================== 暂停 / 恢复 / 取消 ========================

    public void pauseTask(Long taskId, String reason) {
        log.info("暂停任务: taskId={}, reason={}", taskId, reason);
        Task task = queryTaskById(taskId);
        String fromStatus = task.getStatus();
        if (!canPause(fromStatus)) {
            throw new IllegalStateException("任务状态 " + fromStatus + " 不允许暂停");
        }

        task.setStatus(TaskStatusEnum.PAUSED.getCode());
        task.setSubStatus("USER_PAUSED");
        task.setFailureReason(reason);
        task.setUpdateTime(LocalDateTime.now());

        self().doPauseTask(task, fromStatus, reason);
        taskEventPublisher.publishTaskPausedEvent(task, TaskStatusEnum.fromCode(fromStatus));
        log.info("暂停任务成功: taskId={}, fromStatus={}", taskId, fromStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    public void doPauseTask(Task task, String fromStatus, String reason) {
        taskRepository.updateTask(task);
        recordEvent(task.getId(), fromStatus, TaskStatusEnum.PAUSED.getCode(),
                TaskEventTypeEnum.TASK_PAUSED_EVENT, reason);
    }

    public void resumeTask(Long taskId, String wakeupContext) {
        log.info("恢复任务: taskId={}", taskId);
        Task task = queryTaskById(taskId);
        if (!TaskStatusEnum.PAUSED.getCode().equals(task.getStatus())) {
            throw new IllegalStateException("只有 PAUSED 状态的任务才能恢复");
        }

        task.setStatus(TaskStatusEnum.PENDING.getCode());
        task.setSubStatus(null);
        task.setFailureReason(null);
        task.setWakeupContext(wakeupContext);
        task.setUpdateTime(LocalDateTime.now());

        self().doResumeTask(task);
        taskEventPublisher.publishTaskResumedEvent(task);
        log.info("恢复任务成功: taskId={}", taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void doResumeTask(Task task) {
        taskRepository.updateTask(task);
        recordEvent(task.getId(), TaskStatusEnum.PAUSED.getCode(),
                TaskStatusEnum.PENDING.getCode(), TaskEventTypeEnum.TASK_RESUMED_EVENT, "任务恢复");
    }

    public void cancelTask(Long taskId, String reason) {
        log.info("取消任务: taskId={}, reason={}", taskId, reason);
        Task task = queryTaskById(taskId);
        String preStatus = task.getStatus();
        if (TaskStatusEnum.fromCode(preStatus).isFinalStatus()) {
            throw new IllegalStateException("终止状态的任务不能取消");
        }

        task.setStatus(TaskStatusEnum.CANCELLED.getCode());
        task.setSubStatus("USER_CANCELLED");
        task.setFailureReason(reason);
        task.setUpdateTime(LocalDateTime.now());

        self().doCancelTask(task, preStatus, reason);
        taskEventPublisher.publishTaskCancelledEvent(task);
        log.info("取消任务成功: taskId={}, fromStatus={}", taskId, preStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    public void doCancelTask(Task task, String preStatus, String reason) {
        taskRepository.updateTask(task);
        cascadeCancelSubTasks(task.getId(), reason);
        recordEvent(task.getId(), preStatus, TaskStatusEnum.CANCELLED.getCode(),
                TaskEventTypeEnum.TASK_CANCELLED_EVENT, reason);
    }

    // ======================== 完成 / 失败 ========================

    public void completeTask(Long taskId, String outputData, String bizId) {
        log.info("完成任务: taskId={}, bizId={}", taskId, bizId);
        Task task = queryTaskById(taskId);
        if (!TaskStatusEnum.RUNNING.getCode().equals(task.getStatus())) {
            throw new IllegalStateException("任务状态必须为 RUNNING 才能完成");
        }

        task.setStatus(TaskStatusEnum.COMPLETED.getCode());
        task.setSubStatus(null);
        task.setEndTime(LocalDateTime.now());
        task.setOutputData(outputData);
        if (StringUtils.isNotBlank(bizId)) task.setTaskBizId(bizId);
        task.setUpdateTime(LocalDateTime.now());

        self().doCompleteTask(task);
        taskEventPublisher.publishTaskCompletedEvent(task);
        log.info("完成任务成功: taskId={}", taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void doCompleteTask(Task task) {
        taskRepository.updateTask(task);
        recordEvent(task.getId(), TaskStatusEnum.RUNNING.getCode(),
                TaskStatusEnum.COMPLETED.getCode(), TaskEventTypeEnum.TASK_COMPLETED_EVENT, "任务完成");
    }

    public void failTask(Long taskId, String subStatus, String failureReason) {
        log.info("任务失败处理: taskId={}, subStatus={}", taskId, subStatus);
        Task task = queryTaskById(taskId);
        boolean canRetry = canRetry(task);
        String fromStatus = task.getStatus();
        LocalDateTime failedAt = LocalDateTime.now();

        if (canRetry) {
            task.setStatus(TaskStatusEnum.RETRYING.getCode());
            task.setSubStatus(subStatus);
            task.setFailureReason(failureReason);
            task.setErrorMsg(failureReason);
            task.setRetryCount(task.getRetryCount() + 1);
            task.setScheduledTime(calculateNextRetryTime(task));
        } else {
            task.setStatus(TaskStatusEnum.FAILED.getCode());
            task.setSubStatus(subStatus);
            task.setFailureReason(failureReason);
            task.setErrorMsg(failureReason);
            task.setEndTime(LocalDateTime.now());
        }
        task.setUpdateTime(LocalDateTime.now());

        self().doFailTask(task, fromStatus, failureReason, canRetry, failedAt);

        if (canRetry) {
            taskEventPublisher.publishTaskRetryingEvent(task);
            log.info("任务将重试: taskId={}, retryCount={}, nextRetryTime={}",
                    taskId, task.getRetryCount(), task.getScheduledTime());
        } else {
            taskEventPublisher.publishTaskFailedEvent(task);
            log.warn("任务最终失败: taskId={}, subStatus={}", taskId, subStatus);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void doFailTask(Task task, String fromStatus, String failureReason,
                           boolean canRetry, LocalDateTime failedAt) {
        taskRepository.updateTask(task);
        if (canRetry) {
            recordRetryLog(task, task.getSubStatus(), task.getScheduledTime(), failedAt);
        }
        recordEvent(task.getId(), fromStatus, task.getStatus(),
                TaskEventTypeEnum.TASK_FAILED_EVENT, failureReason);
    }

    /**
     * 将 WAITING_SUB_TASK 状态的父任务标记为失败（子任务失败触发）
     */
    public void failWaitingParentTask(Long taskId, String subStatus, String failureReason) {
        log.info("等待子任务的父任务失败: taskId={}, subStatus={}", taskId, subStatus);
        Task task = queryTaskById(taskId);
        if (!TaskStatusEnum.WAITING_SUB_TASK.getCode().equals(task.getStatus())) {
            throw new IllegalStateException(
                    "failWaitingParentTask 仅适用于 WAITING_SUB_TASK 状态，当前：" + task.getStatus());
        }

        task.setStatus(TaskStatusEnum.FAILED.getCode());
        task.setSubStatus(subStatus);
        task.setFailureReason(failureReason);
        task.setErrorMsg(failureReason);
        task.setEndTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        self().doFailWaitingParentTask(task, failureReason);
        taskEventPublisher.publishTaskFailedEvent(task);
        log.warn("等待子任务的父任务已标记失败: taskId={}", taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void doFailWaitingParentTask(Task task, String failureReason) {
        taskRepository.updateTask(task);
        recordEvent(task.getId(), TaskStatusEnum.WAITING_SUB_TASK.getCode(),
                TaskStatusEnum.FAILED.getCode(), TaskEventTypeEnum.TASK_FAILED_EVENT, failureReason);
    }

    // ======================== 等待子任务 ========================

    /**
     * 任务进入等待子任务状态
     *
     * wakeupContext 以 Map 结构存储，key 为 executionRound，追加写入不覆盖历史轮次
     */
    public void waitForSubTasks(Long taskId, WaitStrategyEnum strategy, String wakeupContext) {
        log.info("任务进入等待子任务状态: taskId={}, strategy={}", taskId, strategy);
        Task task = queryTaskById(taskId);
        if (!TaskStatusEnum.RUNNING.getCode().equals(task.getStatus())) {
            throw new IllegalStateException("只有 RUNNING 状态的任务才能等待子任务");
        }

        String mergedContext = mergeWakeupContext(
                task.getWakeupContext(), task.getExecutionRound(), wakeupContext);

        task.setStatus(TaskStatusEnum.WAITING_SUB_TASK.getCode());
        task.setWaitStrategy(strategy);
        task.setWakeupContext(mergedContext);
        task.setUpdateTime(LocalDateTime.now());

        self().doWaitForSubTasks(task, strategy);
        taskEventPublisher.publishTaskWaitingSubTasksEvent(task, 0);
        log.info("任务已进入等待子任务状态: taskId={}, strategy={}", taskId, strategy);
    }

    @Transactional(rollbackFor = Exception.class)
    public void doWaitForSubTasks(Task task, WaitStrategyEnum strategy) {
        taskRepository.updateTask(task);
        recordEvent(task.getId(), TaskStatusEnum.RUNNING.getCode(),
                TaskStatusEnum.WAITING_SUB_TASK.getCode(),
                TaskEventTypeEnum.TASK_WAITING_SUB_TASKS_EVENT,
                "等待子任务完成，策略：" + strategy.getCode());
    }

    // ======================== 手动重试 ========================

    public void manualRetryTask(Long taskId, String operator) {
        taskManualRetryService.manualRetryTask(taskId, operator);
    }

    public Map<Long, String> batchManualRetry(List<Long> taskIds, String operator) {
        return taskManualRetryService.batchManualRetry(taskIds, operator);
    }

    // ======================== 历史查询 ========================

    public List<TaskRetryLog> queryRetryLogs(Long taskId) {
        return taskRetryLogRepository.getRetryLogsByTaskId(taskId);
    }

    public List<com.wheel.cloud.task.entity.TaskEvent> queryTaskEvents(Long taskId) {
        return taskEventRepository.getEventsByTaskId(taskId);
    }

    // ======================== 工具方法 ========================

    /**
     * 从 Map 结构的 wakeupContext 中按执行轮次取对应上下文
     */
    public static String resolveWakeupContext(String wakeupContext, Integer executionRound) {
        if (StringUtils.isBlank(wakeupContext) || executionRound == null) return wakeupContext;
        try {
            Map<String, String> ctx = JsonUtil.parseObject(
                    wakeupContext, new TypeReference<Map<String, String>>() {});
            if (ctx != null) return ctx.get(String.valueOf(executionRound));
        } catch (Exception ignored) {
            // 兼容旧格式（非 Map 的纯字符串），原样返回
        }
        return wakeupContext;
    }

    // ======================== 私有辅助 ========================

    private void setupParentRelation(Task task, Long parentTaskId) {
        Task parent = taskRepository.getTaskById(parentTaskId);
        if (parent == null) throw new IllegalArgumentException("父任务不存在: " + parentTaskId);
        task.setParentId(parent.getId());
        task.setRootId(parent.getRootId() != null ? parent.getRootId() : parent.getId());
        task.setParentExecutionRound(parent.getExecutionRound());
    }

    private void cascadeCancelSubTasks(Long parentId, String reason) {
        List<Task> subTasks = taskRepository.getSubTasks(parentId);
        if (CollectionUtils.isEmpty(subTasks)) return;
        for (Task sub : subTasks) {
            if (!TaskStatusEnum.fromCode(sub.getStatus()).isFinalStatus()) {
                sub.setStatus(TaskStatusEnum.CANCELLED.getCode());
                sub.setSubStatus("PARENT_CANCELLED");
                sub.setFailureReason("父任务[id=" + parentId + "]被取消");
                sub.setUpdateTime(LocalDateTime.now());
                taskRepository.updateTask(sub);
                recordEvent(sub.getId(), null, TaskStatusEnum.CANCELLED.getCode(),
                        TaskEventTypeEnum.TASK_CANCELLED_EVENT, reason);
                taskEventPublisher.publishTaskCancelledEvent(sub);
                log.info("级联取消子任务: subTaskId={}, parentId={}", sub.getId(), parentId);
            }
        }
    }

    private boolean canPause(String status) {
        return TaskStatusEnum.PENDING.getCode().equals(status)
                || TaskStatusEnum.RUNNING.getCode().equals(status)
                || TaskStatusEnum.WAITING_SUB_TASK.getCode().equals(status);
    }

    private boolean canRetry(Task task) {
        int retryCount = task.getRetryCount() != null ? task.getRetryCount() : 0;
        int maxRetry = task.getMaxRetry() != null ? task.getMaxRetry() : 0;
        boolean ok = retryCount < maxRetry;
        if (ok && task.getExpireTime() != null) {
            ok = task.getExpireTime().isAfter(LocalDateTime.now());
        }
        return ok;
    }

    private LocalDateTime calculateNextRetryTime(Task task) {
        TaskTypeConfig config = taskTypeConfigRepository.getByTaskTypeCode(task.getTaskTypeCode());
        if (config == null) return LocalDateTime.now().plusSeconds(60);
        int interval = config.getRetryIntervalSeconds() != null ? config.getRetryIntervalSeconds() : 60;
        if ("EXPONENTIAL_BACKOFF".equals(config.getRetryStrategy())) {
            long multiplier = (long) Math.pow(2, task.getRetryCount() - 1);
            interval = (int) (interval * multiplier);
        }
        return LocalDateTime.now().plusSeconds(interval);
    }

    private void recordRetryLog(Task task, String subStatus,
                                LocalDateTime nextRetryTime, LocalDateTime failedAt) {
        TaskTypeConfig config = taskTypeConfigRepository.getByTaskTypeCode(task.getTaskTypeCode());
        TaskRetryLog log = new TaskRetryLog();
        log.setTaskId(task.getId());
        log.setSessionId(StringUtils.defaultString(task.getSessionId()));
        log.setRetryCount(task.getRetryCount() - 1);
        log.setErrorType(subStatus);
        log.setErrorMsg(StringUtils.defaultString(task.getErrorMsg()));
        log.setStartTime(task.getStartTime());
        log.setEndTime(failedAt);
        log.setNextRetryTime(nextRetryTime);
        log.setRetryStrategy(config != null ? config.getRetryStrategy() : "FIXED");
        log.setCreateTime(LocalDateTime.now());
        taskRetryLogRepository.saveRetryLog(log);
    }

    /**
     * 将本轮 wakeupContext 追加到 Map，key = executionRound
     * 存储格式：{"1":"第1轮上下文","2":"第2轮上下文"}
     */
    private String mergeWakeupContext(String existing, Integer round, String newContext) {
        Map<String, String> ctx = new HashMap<>();
        if (StringUtils.isNotBlank(existing)) {
            try {
                Map<String, String> parsed = JsonUtil.parseObject(
                        existing, new TypeReference<Map<String, String>>() {});
                if (parsed != null) ctx.putAll(parsed);
            } catch (Exception e) {
                log.warn("解析已有 wakeupContext 失败，将直接覆盖, existing={}", existing, e);
            }
        }
        ctx.put(String.valueOf(round), newContext);
        return JsonUtil.toJson(ctx);
    }

    private void recordEvent(Long taskId, String fromStatus, String toStatus,
                             TaskEventTypeEnum eventType, String description) {
        TaskEvent event = new TaskEvent();
        event.setTaskId(taskId);
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setEventType(eventType.getEventType());
        event.setOperator("SYSTEM");
        event.setEventTime(LocalDateTime.now());
        event.setCreateTime(LocalDateTime.now());
        if (description != null) {
            Map<String, Object> data = new HashMap<>(1);
            data.put("description", description);
            event.setEventData(data);
        }
        taskEventRepository.saveTaskEvent(event);
    }

    private TaskTypeConfig loadTypeConfig(String taskTypeCode) {
        TaskTypeConfig config = taskTypeConfigRepository.getByTaskTypeCode(taskTypeCode);
        if (config == null) throw new IllegalArgumentException("任务类型配置不存在: " + taskTypeCode);
        return config;
    }
}
