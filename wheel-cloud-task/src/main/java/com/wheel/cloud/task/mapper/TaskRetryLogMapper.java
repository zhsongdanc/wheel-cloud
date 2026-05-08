package com.wheel.cloud.task.mapper;

import com.wheel.cloud.task.entity.TaskRetryLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskRetryLogMapper {

    int insert(TaskRetryLog log);

    List<TaskRetryLog> selectByTaskId(@Param("taskId") Long taskId);
}
