package com.wheel.cloud.task;

import com.wheel.cloud.task.constant.TaskStatusEnum;
import com.wheel.cloud.task.constant.WaitStrategyEnum;
import com.wheel.cloud.task.entity.Task;
import com.wheel.cloud.task.event.TaskEvent;
import com.wheel.cloud.task.param.CreateTaskParam;
import com.wheel.cloud.task.repository.TaskRepository;
import com.wheel.cloud.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventCapture eventCapture;

    private static final String TYPE_CODE = "TEST_TASK";

    @BeforeEach
    void setUp() {
        eventCapture.clear();
        jdbcTemplate.update("DELETE FROM t_task_retry_log");
        jdbcTemplate.update("DELETE FROM t_task_event");
        jdbcTemplate.update("DELETE FROM t_task");
        jdbcTemplate.update("DELETE FROM t_task_type_config WHERE task_type_code = ?", TYPE_CODE);
        jdbcTemplate.update(
                "INSERT INTO t_task_type_config " +
                "(task_type_code, task_type_name, executor_type, max_retry_count, " +
                "retry_interval_seconds, retry_strategy, priority, enabled, create_time, update_time) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?)",
                TYPE_CODE, "测试任务", "TEST_EXECUTOR",
                2, 10, "FIXED", 50, 1,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    // ======================== 创建任务 ========================

    @Test
    void createTask_shouldBePending() {
        Long taskId = taskService.createTask(buildParam("测试任务-1"));

        Task task = taskService.queryTaskById(taskId);
        assertEquals(TaskStatusEnum.PENDING.getCode(), task.getStatus());
        assertEquals("测试任务-1", task.getTaskName());
        assertEquals(TYPE_CODE, task.getTaskTypeCode());
        assertEquals(0, task.getRetryCount());
        assertEquals(1, eventCapture.count("TaskCreatedEvent"));
    }

    // ======================== failTask：有重试次数 ========================

    @Test
    void failTask_withRetryLeft_shouldBeRetrying() {
        Long taskId = taskService.createTask(buildParam("重试任务"));
        forceToRunning(taskId);

        taskService.failTask(taskId, "BIZ_ERROR", "业务异常");

        Task task = taskService.queryTaskById(taskId);
        assertEquals(TaskStatusEnum.RETRYING.getCode(), task.getStatus());
        assertEquals(1, task.getRetryCount());
        assertNotNull(task.getScheduledTime());
    }

    // ======================== failTask：无重试次数 ========================

    @Test
    void failTask_noRetryLeft_shouldBeFailed() {
        Long taskId = taskService.createTask(buildParam("无重试任务"));

        // maxRetry=2，消耗 2 次后第 3 次失败
        forceToRunning(taskId);
        taskService.failTask(taskId, "ERR", "fail1");

        forceToRunning(taskId);
        taskService.failTask(taskId, "ERR", "fail2");

        forceToRunning(taskId);
        taskService.failTask(taskId, "ERR", "fail3");

        Task task = taskService.queryTaskById(taskId);
        assertEquals(TaskStatusEnum.FAILED.getCode(), task.getStatus());
        assertNotNull(task.getEndTime());
    }

    // ======================== manualRetryTask ========================

    @Test
    void manualRetryTask_fromFailed_shouldBePending() {
        Long taskId = taskService.createTask(buildParam("手动重试任务"));
        // 耗尽重试次数进入 FAILED
        forceToRunning(taskId); taskService.failTask(taskId, "ERR", "f1");
        forceToRunning(taskId); taskService.failTask(taskId, "ERR", "f2");
        forceToRunning(taskId); taskService.failTask(taskId, "ERR", "f3");
        assertEquals(TaskStatusEnum.FAILED.getCode(), taskService.queryTaskById(taskId).getStatus());

        eventCapture.clear();
        taskService.manualRetryTask(taskId, "admin");

        Task task = taskService.queryTaskById(taskId);
        assertEquals(TaskStatusEnum.PENDING.getCode(), task.getStatus());
        assertEquals(0, task.getRetryCount());
        assertNull(task.getStartTime());
        assertNull(task.getExpireTime());
        assertEquals(1, eventCapture.count("TaskManualRetriedEvent"));
    }

    // ======================== cancelTask：级联取消子任务 ========================

    @Test
    void cancelTask_shouldCascadeCancelSubTasks() {
        Long parentId = taskService.createTask(buildParam("父任务"));
        Long subId = taskService.createTask(
                buildParam("子任务").toBuilder().parentTaskId(parentId).build());

        taskService.cancelTask(parentId, "测试取消");

        assertEquals(TaskStatusEnum.CANCELLED.getCode(),
                taskService.queryTaskById(parentId).getStatus());
        Task sub = taskService.queryTaskById(subId);
        assertEquals(TaskStatusEnum.CANCELLED.getCode(), sub.getStatus());
        assertEquals("PARENT_CANCELLED", sub.getSubStatus());
    }

    // ======================== waitForSubTasks ========================

    @Test
    void waitForSubTasks_shouldTransitionCorrectly() {
        Long taskId = taskService.createTask(buildParam("等待子任务"));
        forceToRunning(taskId);

        taskService.waitForSubTasks(taskId, WaitStrategyEnum.ALL_SUCCESS, "ctx-1");

        Task task = taskService.queryTaskById(taskId);
        assertEquals(TaskStatusEnum.WAITING_SUB_TASK.getCode(), task.getStatus());
        assertEquals(WaitStrategyEnum.ALL_SUCCESS, task.getWaitStrategy());
        assertTrue(task.getWakeupContext().contains("ctx-1"));
    }

    // ======================== resolveWakeupContext ========================

    @Test
    void resolveWakeupContext_shouldExtractCorrectRound() {
        String ctx = "{\"1\":\"round1-ctx\",\"2\":\"round2-ctx\"}";
        assertEquals("round1-ctx", TaskService.resolveWakeupContext(ctx, 1));
        assertEquals("round2-ctx", TaskService.resolveWakeupContext(ctx, 2));
        assertNull(TaskService.resolveWakeupContext(ctx, 99));
        assertNull(TaskService.resolveWakeupContext(null, 1));
    }

    // ======================== 工具方法 ========================

    private CreateTaskParam buildParam(String name) {
        return CreateTaskParam.builder()
                .taskName(name)
                .taskTypeCode(TYPE_CODE)
                .build();
    }

    private void forceToRunning(Long taskId) {
        Task task = taskService.queryTaskById(taskId);
        task.setStatus(TaskStatusEnum.RUNNING.getCode());
        task.setStartTime(LocalDateTime.now());
        taskRepository.updateTask(task);
    }

    // ======================== Spring Boot 测试配置 ========================

    @SpringBootApplication
    @MapperScan("com.wheel.cloud.task.mapper")
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        EventCapture eventCapture() {
            return new EventCapture();
        }
    }

    static class EventCapture implements ApplicationListener<TaskEvent> {
        private final List<TaskEvent> captured = new ArrayList<>();

        @Override
        public void onApplicationEvent(TaskEvent event) {
            captured.add(event);
        }

        int count(String eventType) {
            return (int) captured.stream()
                    .filter(e -> eventType.equals(e.getEventType().getEventType()))
                    .count();
        }

        void clear() { captured.clear(); }
    }
}
