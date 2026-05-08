package com.wheel.cloud.task.repository;

import com.wheel.cloud.task.entity.TaskEvent;
import com.wheel.cloud.task.mapper.TaskEventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskEventRepository {

    @Autowired
    private TaskEventMapper taskEventMapper;

    public void saveTaskEvent(TaskEvent event) {
        taskEventMapper.insert(event);
    }

    public List<TaskEvent> getEventsByTaskId(Long taskId) {
        return taskEventMapper.selectByTaskId(taskId);
    }
}
