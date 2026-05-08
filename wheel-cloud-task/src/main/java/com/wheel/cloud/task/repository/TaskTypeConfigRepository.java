package com.wheel.cloud.task.repository;

import com.wheel.cloud.task.entity.TaskTypeConfig;
import com.wheel.cloud.task.mapper.TaskTypeConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class TaskTypeConfigRepository {

    @Autowired
    private TaskTypeConfigMapper taskTypeConfigMapper;

    public TaskTypeConfig getByTaskTypeCode(String taskTypeCode) {
        return taskTypeConfigMapper.selectByTaskTypeCode(taskTypeCode);
    }

    public Map<String, TaskTypeConfig> getMapByTaskTypeCodes(Collection<String> taskTypeCodes) {
        if (taskTypeCodes == null || taskTypeCodes.isEmpty()) return Collections.emptyMap();
        List<TaskTypeConfig> list = taskTypeConfigMapper.selectByTaskTypeCodes(taskTypeCodes);
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        return list.stream().collect(
                java.util.stream.Collectors.toMap(TaskTypeConfig::getTaskTypeCode, c -> c));
    }
}
