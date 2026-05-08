package com.wheel.cloud.task.repository;

import com.wheel.cloud.task.entity.Task;
import com.wheel.cloud.task.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Repository
public class TaskRepository {

    @Autowired
    private TaskMapper taskMapper;

    public Long createTask(Task task) {
        if (task.getCreateTime() == null) task.setCreateTime(LocalDateTime.now());
        if (task.getUpdateTime() == null) task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);
        return task.getId();
    }

    public void updateTask(Task task) {
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.update(task);
    }

    public void updateTaskIncludeNull(Task task) {
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateIncludeNull(task);
    }

    public Task getTaskById(Long id) {
        return taskMapper.selectById(id);
    }

    public Task getTaskByBizId(String taskBizId) {
        return taskMapper.selectByBizIdOne(taskBizId);
    }

    public List<Task> getSubTasks(Long parentId) {
        return taskMapper.selectByParentId(parentId);
    }

    public List<Task> queryTasksByBizId(String taskBizId) {
        return taskMapper.selectByBizIdAndTaskType(taskBizId, null);
    }

    public List<Task> queryTasksByBizIdAndTaskType(String taskBizId, String taskTypeCode) {
        return taskMapper.selectByBizIdAndTaskType(taskBizId, taskTypeCode);
    }

    public List<Task> getSchedulablePendingTasks(LocalDateTime scheduledBefore, int limit) {
        return taskMapper.selectSchedulablePending(scheduledBefore, limit);
    }

    public List<Task> getPendingRetryTasks(int limit) {
        return taskMapper.selectPendingRetry(limit);
    }

    public List<Task> getAbnormalRunningTasks(LocalDateTime threshold, int limit) {
        return taskMapper.selectAbnormalRunning(threshold, limit);
    }

    public List<Task> getTasksByStatus(String status, int offset, int limit) {
        return taskMapper.selectByStatus(status, offset, limit);
    }
}
