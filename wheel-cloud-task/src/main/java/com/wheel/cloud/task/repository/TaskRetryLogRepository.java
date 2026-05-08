package com.wheel.cloud.task.repository;

import com.wheel.cloud.task.entity.TaskRetryLog;
import com.wheel.cloud.task.mapper.TaskRetryLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskRetryLogRepository {

    @Autowired
    private TaskRetryLogMapper taskRetryLogMapper;

    public void saveRetryLog(TaskRetryLog log) {
        taskRetryLogMapper.insert(log);
    }

    public List<TaskRetryLog> getRetryLogsByTaskId(Long taskId) {
        return taskRetryLogMapper.selectByTaskId(taskId);
    }
}
